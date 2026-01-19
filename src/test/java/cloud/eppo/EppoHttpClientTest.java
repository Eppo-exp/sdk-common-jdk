package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EppoHttpClientTest {
  private MockWebServer mockWebServer;
  private EppoHttpClient httpClient;
  private static final String TEST_API_KEY = "test-secret-api-key-12345";
  private static final String SDK_NAME = "test-sdk";
  private static final String SDK_VERSION = "1.0.0";

  @BeforeEach
  public void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();
    httpClient = new EppoHttpClient(baseUrl, TEST_API_KEY, SDK_NAME, SDK_VERSION);
  }

  @AfterEach
  public void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  public void testApiKeyRedactedInIOExceptionMessage() {
    // Simulate a response that will cause an IOException when reading the body
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("test")
            .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));

    CompletableFuture<EppoHttpResponse> future = httpClient.getAsync("/test-path");

    ExecutionException exception = assertThrows(ExecutionException.class, future::get);
    Throwable cause = exception.getCause();
    assertNotNull(cause);
    String errorMessage = cause.getMessage();

    // Verify the error message contains a URL
    assertTrue(errorMessage.contains("URL"), "Error message should mention URL");

    // Verify the actual API key is NOT present in the error message
    assertFalse(
        errorMessage.contains(TEST_API_KEY), "Error message should not contain the actual API key");

    // Verify the redacted placeholder IS present
    assertTrue(
        errorMessage.contains("apiKey=<redacted>"),
        "Error message should contain redacted API key placeholder");
  }

  @Test
  public void testApiKeyRedactedInBadResponseMessage() {
    // Return a 500 error to trigger the "Bad response" error path
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

    CompletableFuture<EppoHttpResponse> future = httpClient.getAsync("/test-path");

    ExecutionException exception = assertThrows(ExecutionException.class, future::get);
    Throwable cause = exception.getCause();
    assertNotNull(cause);
    String errorMessage = cause.getMessage();

    // Verify the error message is about a bad response
    assertTrue(
        errorMessage.contains("Bad response from URL"),
        "Error message should mention bad response");

    // Verify the actual API key is NOT present in the error message
    assertFalse(
        errorMessage.contains(TEST_API_KEY), "Error message should not contain the actual API key");

    // Verify the redacted placeholder IS present
    assertTrue(
        errorMessage.contains("apiKey=<redacted>"),
        "Error message should contain redacted API key placeholder");
  }

  @Test
  public void testApiKeyRedactedInConnectionFailureMessage() throws IOException {
    // Shut down the server to simulate connection failure
    mockWebServer.shutdown();

    CompletableFuture<EppoHttpResponse> future = httpClient.getAsync("/test-path");

    ExecutionException exception = assertThrows(ExecutionException.class, future::get);
    Throwable cause = exception.getCause();
    assertNotNull(cause);
    String errorMessage = cause.getMessage();

    // Verify the error message is about being unable to fetch
    assertTrue(
        errorMessage.contains("Unable to fetch from URL"),
        "Error message should mention unable to fetch");

    // Verify the actual API key is NOT present in the error message
    assertFalse(
        errorMessage.contains(TEST_API_KEY), "Error message should not contain the actual API key");

    // Verify the redacted placeholder IS present
    assertTrue(
        errorMessage.contains("apiKey=<redacted>"),
        "Error message should contain redacted API key placeholder");
  }

  @Test
  public void testApiKeyNotRedactedInSuccessfulRequest()
      throws ExecutionException, InterruptedException {
    // Return a successful response
    String responseBody = "success response";
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(responseBody));

    CompletableFuture<EppoHttpResponse> future = httpClient.getAsync("/test-path");
    EppoHttpResponse response = future.get();

    // Verify the request was successful
    assertNotNull(response);
    assertEquals(200, response.getStatusCode());
    assertEquals(responseBody, new String(response.getBody()));
  }

  @Test
  public void testInvalidApiKeyError() {
    // Return a 403 error to trigger the "Invalid API key" error path
    mockWebServer.enqueue(new MockResponse().setResponseCode(403).setBody("Forbidden"));

    CompletableFuture<EppoHttpResponse> future = httpClient.getAsync("/test-path");

    ExecutionException exception = assertThrows(ExecutionException.class, future::get);
    Throwable cause = exception.getCause();
    assertNotNull(cause);
    String errorMessage = cause.getMessage();

    // For 403 errors, the message should be "Invalid API key" without URL details
    assertEquals(
        "Invalid API key",
        errorMessage,
        "403 errors should show generic 'Invalid API key' message");
  }

  @Test
  public void testApiKeyRedactionPreservesOtherQueryParameters() {
    // Return a 500 error to trigger error path
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));

    CompletableFuture<EppoHttpResponse> future = httpClient.getAsync("/test-path");

    ExecutionException exception = assertThrows(ExecutionException.class, future::get);
    Throwable cause = exception.getCause();
    assertNotNull(cause);
    String errorMessage = cause.getMessage();

    // Verify other query parameters are still present
    assertTrue(errorMessage.contains("sdkName=" + SDK_NAME), "SDK name should be preserved");
    assertTrue(
        errorMessage.contains("sdkVersion=" + SDK_VERSION), "SDK version should be preserved");

    // Verify API key is redacted
    assertFalse(errorMessage.contains(TEST_API_KEY), "API key should be redacted");
    assertTrue(
        errorMessage.contains("apiKey=<redacted>"), "Redacted placeholder should be present");
  }
}

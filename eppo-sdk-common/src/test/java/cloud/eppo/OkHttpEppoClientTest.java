package cloud.eppo;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.http.EppoConfigurationRequest;
import cloud.eppo.http.EppoConfigurationResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OkHttpEppoClientTest {
  private MockWebServer mockWebServer;
  private OkHttpEppoClient client;
  private String baseUrl;

  @BeforeEach
  public void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    baseUrl = mockWebServer.url("").toString();
    // Remove trailing slash if present
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    client = new OkHttpEppoClient();
  }

  @AfterEach
  public void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  public void testSuccessfulGet() throws ExecutionException, InterruptedException {
    String responseBody = "{\"flags\": {}}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("ETag", "v1")
            .setBody(responseBody));

    EppoConfigurationRequest request = createRequest(null);
    CompletableFuture<EppoConfigurationResponse> future = client.execute(request);
    EppoConfigurationResponse response = future.get();

    assertTrue(response.isSuccessful());
    assertFalse(response.isNotModified());
    assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
    assertEquals("v1", response.getVersionId());
    assertThat(new String(response.getBody())).isEqualTo(responseBody);
  }

  @Test
  public void testNotModifiedResponse() throws ExecutionException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_NOT_MODIFIED)
            .setHeader("ETag", "v1"));

    EppoConfigurationRequest request = createRequest("v1");
    CompletableFuture<EppoConfigurationResponse> future = client.execute(request);
    EppoConfigurationResponse response = future.get();

    assertTrue(response.isNotModified());
    assertFalse(response.isSuccessful());
    assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, response.getStatusCode());
    assertEquals("v1", response.getVersionId());
    assertNull(response.getBody());
  }

  @Test
  public void testConditionalRequestSendsIfNoneMatchHeader()
      throws ExecutionException, InterruptedException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_NOT_MODIFIED)
            .setHeader("ETag", "v1"));

    EppoConfigurationRequest request = createRequest("v1");
    client.execute(request).get();

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("v1", recordedRequest.getHeader("If-None-Match"));
  }

  @Test
  public void testNoIfNoneMatchHeaderWhenNoVersionId()
      throws ExecutionException, InterruptedException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("{}"));

    EppoConfigurationRequest request = createRequest(null);
    client.execute(request).get();

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertNull(recordedRequest.getHeader("If-None-Match"));
  }

  @Test
  public void testQueryParametersAreIncluded()
      throws ExecutionException, InterruptedException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("{}"));

    EppoConfigurationRequest request = createRequest(null);
    client.execute(request).get();

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    String path = recordedRequest.getPath();
    assertThat(path).contains("apiKey=test-key");
    assertThat(path).contains("sdkName=test-sdk");
    assertThat(path).contains("sdkVersion=1.0.0");
  }

  @Test
  public void testErrorResponse() throws ExecutionException, InterruptedException {
    String errorBody = "Internal Server Error";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
            .setBody(errorBody));

    EppoConfigurationRequest request = createRequest(null);
    CompletableFuture<EppoConfigurationResponse> future = client.execute(request);
    EppoConfigurationResponse response = future.get();

    assertFalse(response.isSuccessful());
    assertFalse(response.isNotModified());
    assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, response.getStatusCode());
    assertThat(new String(response.getBody())).isEqualTo(errorBody);
  }

  @Test
  public void testForbiddenResponse() throws ExecutionException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpURLConnection.HTTP_FORBIDDEN).setBody("Forbidden"));

    EppoConfigurationRequest request = createRequest(null);
    CompletableFuture<EppoConfigurationResponse> future = client.execute(request);
    EppoConfigurationResponse response = future.get();

    assertFalse(response.isSuccessful());
    assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
  }

  @Test
  public void testConnectionFailure() throws IOException {
    mockWebServer.shutdown();

    EppoConfigurationRequest request = createRequest(null);
    CompletableFuture<EppoConfigurationResponse> future = client.execute(request);

    ExecutionException exception = assertThrows(ExecutionException.class, future::get);
    assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
    assertThat(exception.getCause().getMessage()).contains("Unable to fetch from URL");
  }

  @Test
  public void testApiKeyRedactedInErrorMessage() throws IOException {
    mockWebServer.shutdown();

    EppoConfigurationRequest request = createRequest(null);
    CompletableFuture<EppoConfigurationResponse> future = client.execute(request);

    ExecutionException exception = assertThrows(ExecutionException.class, future::get);
    String errorMessage = exception.getCause().getMessage();

    assertThat(errorMessage).doesNotContain("test-key");
    assertThat(errorMessage).contains("apiKey=<redacted>");
  }

  @Test
  public void testETagExtractedFromResponse() throws ExecutionException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("ETag", "\"abc123\"")
            .setBody("{}"));

    EppoConfigurationRequest request = createRequest(null);
    EppoConfigurationResponse response = client.execute(request).get();

    assertEquals("\"abc123\"", response.getVersionId());
  }

  @Test
  public void testNoETagInResponse() throws ExecutionException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("{}"));

    EppoConfigurationRequest request = createRequest(null);
    EppoConfigurationResponse response = client.execute(request).get();

    assertNull(response.getVersionId());
  }

  @Test
  public void testSuccessfulPostWithJsonBody()
      throws ExecutionException, InterruptedException, InterruptedException {
    String responseBody = "{\"result\": \"success\"}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("ETag", "v2")
            .setBody(responseBody));

    String requestBody = "{\"subjectKey\": \"user-123\", \"subjectAttributes\": {}}";
    EppoConfigurationRequest request =
        new EppoConfigurationRequest.Builder(baseUrl, "/api/assignments")
            .queryParam("apiKey", "test-key")
            .queryParam("sdkName", "test-sdk")
            .queryParam("sdkVersion", "1.0.0")
            .post()
            .jsonBody(requestBody)
            .build();

    CompletableFuture<EppoConfigurationResponse> future = client.execute(request);
    EppoConfigurationResponse response = future.get();

    assertTrue(response.isSuccessful());
    assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
    assertEquals("v2", response.getVersionId());
    assertThat(new String(response.getBody())).isEqualTo(responseBody);

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("POST", recordedRequest.getMethod());
    assertEquals("application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"));
    assertEquals(requestBody, recordedRequest.getBody().readUtf8());
  }

  @Test
  public void testPostWithEmptyBody() throws ExecutionException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("{}"));

    EppoConfigurationRequest request =
        new EppoConfigurationRequest.Builder(baseUrl, "/api/endpoint")
            .queryParam("apiKey", "test-key")
            .post()
            .build();

    CompletableFuture<EppoConfigurationResponse> future = client.execute(request);
    EppoConfigurationResponse response = future.get();

    assertTrue(response.isSuccessful());

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("POST", recordedRequest.getMethod());
    assertEquals(0, recordedRequest.getBodySize());
  }

  @Test
  public void testPostWithCustomContentType()
      throws ExecutionException, InterruptedException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("{}"));

    String requestBody = "key=value&foo=bar";
    EppoConfigurationRequest request =
        new EppoConfigurationRequest.Builder(baseUrl, "/api/endpoint")
            .queryParam("apiKey", "test-key")
            .post()
            .body(requestBody)
            .contentType("application/x-www-form-urlencoded")
            .build();

    client.execute(request).get();

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("POST", recordedRequest.getMethod());
    assertEquals("application/x-www-form-urlencoded", recordedRequest.getHeader("Content-Type"));
    assertEquals(requestBody, recordedRequest.getBody().readUtf8());
  }

  @Test
  public void testPostWithBytesBody()
      throws ExecutionException, InterruptedException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("{}"));

    byte[] requestBody = new byte[] {0x01, 0x02, 0x03, 0x04};
    EppoConfigurationRequest request =
        new EppoConfigurationRequest.Builder(baseUrl, "/api/endpoint")
            .queryParam("apiKey", "test-key")
            .post()
            .body(requestBody)
            .contentType("application/octet-stream")
            .build();

    client.execute(request).get();

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("POST", recordedRequest.getMethod());
    assertArrayEquals(requestBody, recordedRequest.getBody().readByteArray());
  }

  @Test
  public void testExecuteWithGetRequest() throws ExecutionException, InterruptedException {
    String responseBody = "{\"flags\": {}}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("ETag", "v1")
            .setBody(responseBody));

    EppoConfigurationRequest request =
        new EppoConfigurationRequest.Builder(baseUrl, "/api/flag-config/v1/config")
            .queryParam("apiKey", "test-key")
            .queryParam("sdkName", "test-sdk")
            .build();

    CompletableFuture<EppoConfigurationResponse> future = client.execute(request);
    EppoConfigurationResponse response = future.get();

    assertTrue(response.isSuccessful());

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", recordedRequest.getMethod());
  }

  @Test
  public void testBuilderLastVersionId()
      throws ExecutionException, InterruptedException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_NOT_MODIFIED)
            .setHeader("ETag", "v2"));

    EppoConfigurationRequest request =
        new EppoConfigurationRequest.Builder(baseUrl, "/api/config")
            .queryParam("apiKey", "test-key")
            .lastVersionId("v2")
            .build();

    EppoConfigurationResponse response = client.execute(request).get();

    assertTrue(response.isNotModified());

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("v2", recordedRequest.getHeader("If-None-Match"));
  }

  @Test
  public void testStaticGetFactoryMethod() throws ExecutionException, InterruptedException {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("{}"));

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("apiKey", "test-key");

    EppoConfigurationRequest request =
        EppoConfigurationRequest.get(baseUrl, "/api/config", queryParams, null);

    EppoConfigurationResponse response = client.execute(request).get();

    assertTrue(response.isSuccessful());

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", recordedRequest.getMethod());
  }

  private EppoConfigurationRequest createRequest(String lastVersionId) {
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("apiKey", "test-key");
    queryParams.put("sdkName", "test-sdk");
    queryParams.put("sdkVersion", "1.0.0");

    return new EppoConfigurationRequest(
        baseUrl, "/api/flag-config/v1/config", queryParams, lastVersionId);
  }
}

package cloud.eppo.api.configuration;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.IBanditParametersResponse;
import cloud.eppo.api.IFlagConfigResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultEppoConfigurationHttpClientTest {

  private MockWebServer mockWebServer;
  private DefaultEppoConfigurationHttpClient client;

  @BeforeEach
  public void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    client = new DefaultEppoConfigurationHttpClient();
  }

  @AfterEach
  public void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  public void testFetchFlagConfiguration_Success() throws Exception {
    String responseJson =
        "{\"flags\":{\"test-flag\":{\"key\":\"test-flag\",\"enabled\":true,\"variationType\":\"STRING\",\"variations\":{\"control\":{\"key\":\"control\",\"value\":\"control\"}},\"allocations\":[],\"totalShards\":10000}},\"banditReferences\":{},\"format\":\"SERVER\"}";

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("ETag", "test-etag")
            .setBody(responseJson));

    String url = mockWebServer.url("/test").toString();
    ConfigurationRequest request = new ConfigurationRequest(url, "test-key", "java", "1.0.0", null);

    CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> future =
        client.fetchFlagConfiguration(request);
    ConfigurationResponse<IFlagConfigResponse> response = future.get();

    assertTrue(response.isSuccess());
    assertFalse(response.isError());
    assertFalse(response.isNotModified());
    assertEquals(200, response.getStatusCode());
    assertEquals("test-etag", response.getETag());
    assertNotNull(response.getPayload());
    assertNotNull(response.getPayload().getFlags());

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertTrue(recordedRequest.getPath().contains("apiKey=test-key"));
    assertTrue(recordedRequest.getPath().contains("sdkName=java"));
    assertTrue(recordedRequest.getPath().contains("sdkVersion=1.0.0"));
  }

  @Test
  public void testFetchFlagConfiguration_NotModified() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(304).setHeader("ETag", "cached-etag"));

    String url = mockWebServer.url("/test").toString();
    ConfigurationRequest request =
        new ConfigurationRequest(url, "test-key", "java", "1.0.0", "cached-etag");

    CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> future =
        client.fetchFlagConfiguration(request);
    ConfigurationResponse<IFlagConfigResponse> response = future.get();

    assertTrue(response.isNotModified());
    assertFalse(response.isError());
    assertFalse(response.isSuccess());
    assertEquals(304, response.getStatusCode());
    assertEquals("cached-etag", response.getETag());
    assertNull(response.getPayload());

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("cached-etag", recordedRequest.getHeader("If-None-Match"));
  }

  @Test
  public void testFetchFlagConfiguration_InvalidApiKey() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(403));

    String url = mockWebServer.url("/test").toString();
    ConfigurationRequest request =
        new ConfigurationRequest(url, "invalid-key", "java", "1.0.0", null);

    CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> future =
        client.fetchFlagConfiguration(request);
    ConfigurationResponse<IFlagConfigResponse> response = future.get();

    assertTrue(response.isError());
    assertFalse(response.isSuccess());
    assertFalse(response.isNotModified());
    assertEquals(403, response.getStatusCode());
    assertEquals("Invalid API key", response.getErrorMessage());
    assertNull(response.getPayload());
  }

  @Test
  public void testFetchFlagConfiguration_ServerError() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal server error"));

    String url = mockWebServer.url("/test").toString();
    ConfigurationRequest request = new ConfigurationRequest(url, "test-key", "java", "1.0.0", null);

    CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> future =
        client.fetchFlagConfiguration(request);
    ConfigurationResponse<IFlagConfigResponse> response = future.get();

    assertTrue(response.isError());
    assertEquals(500, response.getStatusCode());
    assertTrue(response.getErrorMessage().contains("500"));
    assertNull(response.getPayload());
  }

  @Test
  public void testFetchFlagConfiguration_JsonParsingError() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("ETag", "test-etag")
            .setBody("invalid json"));

    String url = mockWebServer.url("/test").toString();
    ConfigurationRequest request = new ConfigurationRequest(url, "test-key", "java", "1.0.0", null);

    CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> future =
        client.fetchFlagConfiguration(request);
    ConfigurationResponse<IFlagConfigResponse> response = future.get();

    assertTrue(response.isError());
    assertEquals(500, response.getStatusCode());
    assertTrue(response.getErrorMessage().contains("JSON parsing error"));
    assertNull(response.getPayload());
  }

  @Test
  public void testFetchBanditConfiguration_Success() throws Exception {
    String responseJson =
        "{\"bandits\":{\"test-bandit\":{\"banditKey\":\"test-bandit\",\"modelName\":\"test-model\",\"modelVersion\":\"v1\",\"updatedAt\":\"2023-01-01T00:00:00.000Z\",\"modelData\":{\"gamma\":1.0,\"defaultActionScore\":0.0,\"actionProbabilityFloor\":0.0,\"coefficients\":{}}}}}";

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("ETag", "bandit-etag")
            .setBody(responseJson));

    String url = mockWebServer.url("/test").toString();
    ConfigurationRequest request = new ConfigurationRequest(url, "test-key", "java", "1.0.0", null);

    CompletableFuture<ConfigurationResponse<IBanditParametersResponse>> future =
        client.fetchBanditConfiguration(request);
    ConfigurationResponse<IBanditParametersResponse> response = future.get();

    assertTrue(response.isSuccess());
    assertFalse(response.isError());
    assertFalse(response.isNotModified());
    assertEquals(200, response.getStatusCode());
    assertEquals("bandit-etag", response.getETag());
    assertNotNull(response.getPayload());
    assertNotNull(response.getPayload().getBandits());
  }

  @Test
  public void testFetchBanditConfiguration_NotModified() throws Exception {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(304).setHeader("ETag", "cached-bandit-etag"));

    String url = mockWebServer.url("/test").toString();
    ConfigurationRequest request =
        new ConfigurationRequest(url, "test-key", "java", "1.0.0", "cached-bandit-etag");

    CompletableFuture<ConfigurationResponse<IBanditParametersResponse>> future =
        client.fetchBanditConfiguration(request);
    ConfigurationResponse<IBanditParametersResponse> response = future.get();

    assertTrue(response.isNotModified());
    assertFalse(response.isError());
    assertFalse(response.isSuccess());
    assertEquals(304, response.getStatusCode());
    assertEquals("cached-bandit-etag", response.getETag());
    assertNull(response.getPayload());

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("cached-bandit-etag", recordedRequest.getHeader("If-None-Match"));
  }

  @Test
  public void testFetchConfiguration_WithoutETag() throws Exception {
    String responseJson = "{\"flags\":{},\"banditReferences\":{},\"format\":\"SERVER\"}";

    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(responseJson));

    String url = mockWebServer.url("/test").toString();
    ConfigurationRequest request = new ConfigurationRequest(url, "test-key", "java", "1.0.0", null);

    CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> future =
        client.fetchFlagConfiguration(request);
    ConfigurationResponse<IFlagConfigResponse> response = future.get();

    assertTrue(response.isSuccess());
    assertNull(response.getETag());

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertNull(recordedRequest.getHeader("If-None-Match"));
  }

  @Test
  public void testApiKeyRedaction() throws Exception {
    // This test verifies that API keys are redacted in error messages
    // We'll trigger a network error by shutting down the server
    mockWebServer.shutdown();

    String url = mockWebServer.url("/test").toString();
    ConfigurationRequest request =
        new ConfigurationRequest(url, "secret-api-key", "java", "1.0.0", null);

    CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> future =
        client.fetchFlagConfiguration(request);
    ConfigurationResponse<IFlagConfigResponse> response = future.get();

    assertTrue(response.isError());
    assertEquals(500, response.getStatusCode());
    // Error message should not contain the actual API key
    assertFalse(response.getErrorMessage().contains("secret-api-key"));
  }

  @Test
  public void testFetchConfiguration_EmptyResponseBody() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    String url = mockWebServer.url("/test").toString();
    ConfigurationRequest request = new ConfigurationRequest(url, "test-key", "java", "1.0.0", null);

    CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> future =
        client.fetchFlagConfiguration(request);
    ConfigurationResponse<IFlagConfigResponse> response = future.get();

    assertTrue(response.isError());
    assertEquals(500, response.getStatusCode());
  }
}

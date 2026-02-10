package cloud.eppo.helpers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import cloud.eppo.BaseEppoClient;
import cloud.eppo.EppoHttpClient;
import cloud.eppo.http.EppoConfigurationClient;
import cloud.eppo.http.EppoConfigurationRequest;
import cloud.eppo.http.EppoConfigurationResponse;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

public class TestUtils {

  /**
   * Creates a mock EppoConfigurationClient that returns the given response body for all requests.
   *
   * @param responseBody the response body to return
   * @return a mock EppoConfigurationClient
   */
  public static EppoConfigurationClient mockConfigurationClient(String responseBody) {
    return mockConfigurationClient(responseBody.getBytes());
  }

  /**
   * Creates a mock EppoConfigurationClient that returns the given response body for all requests.
   *
   * @param responseBody the response body to return
   * @return a mock EppoConfigurationClient
   */
  public static EppoConfigurationClient mockConfigurationClient(byte[] responseBody) {
    EppoConfigurationClient mockClient = mock(EppoConfigurationClient.class);
    EppoConfigurationResponse successResponse =
        EppoConfigurationResponse.success(200, "test-version", responseBody);

    when(mockClient.get(any(EppoConfigurationRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(successResponse));

    return mockClient;
  }

  /**
   * Creates a mock EppoConfigurationClient that returns an error for all requests.
   *
   * @return a mock EppoConfigurationClient that fails
   */
  public static EppoConfigurationClient mockConfigurationClientError() {
    EppoConfigurationClient mockClient = mock(EppoConfigurationClient.class);

    CompletableFuture<EppoConfigurationResponse> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Intentional Error"));

    when(mockClient.get(any(EppoConfigurationRequest.class))).thenReturn(failedFuture);

    return mockClient;
  }

  /**
   * Creates a mock EppoConfigurationClient that returns a 500 error response.
   *
   * @return a mock EppoConfigurationClient that returns error status
   */
  public static EppoConfigurationClient mockConfigurationClientErrorResponse() {
    EppoConfigurationClient mockClient = mock(EppoConfigurationClient.class);
    EppoConfigurationResponse errorResponse =
        EppoConfigurationResponse.error(500, "Internal Server Error".getBytes());

    when(mockClient.get(any(EppoConfigurationRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(errorResponse));

    return mockClient;
  }

  // ==================== Legacy methods (deprecated, use mockConfigurationClient instead)
  // ====================

  /** @deprecated Use mockConfigurationClient() and pass to BaseEppoClient constructor instead */
  @Deprecated
  @SuppressWarnings("SameParameterValue")
  public static EppoHttpClient mockHttpResponse(String responseBody) {
    // Create a mock instance of EppoHttpClient
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    // Mock sync get
    when(mockHttpClient.get(anyString())).thenReturn(responseBody.getBytes());

    // Mock async get
    CompletableFuture<byte[]> mockAsyncResponse = new CompletableFuture<>();
    when(mockHttpClient.getAsync(anyString())).thenReturn(mockAsyncResponse);
    mockAsyncResponse.complete(responseBody.getBytes());

    setBaseClientHttpClientOverrideField(mockHttpClient);
    return mockHttpClient;
  }

  /**
   * @deprecated Use mockConfigurationClientError() and pass to BaseEppoClient constructor instead
   */
  @Deprecated
  public static void mockHttpError() {
    // Create a mock instance of EppoHttpClient
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    // Mock sync get
    when(mockHttpClient.get(anyString())).thenThrow(new RuntimeException("Intentional Error"));

    // Mock async get
    CompletableFuture<byte[]> mockAsyncResponse = new CompletableFuture<>();
    when(mockHttpClient.getAsync(anyString())).thenReturn(mockAsyncResponse);
    mockAsyncResponse.completeExceptionally(new RuntimeException("Intentional Error"));

    setBaseClientHttpClientOverrideField(mockHttpClient);
  }

  /** @deprecated Will be removed when httpClientOverride is removed from BaseEppoClient */
  @Deprecated
  public static void setBaseClientHttpClientOverrideField(EppoHttpClient httpClient) {
    setBaseClientOverrideField("httpClientOverride", httpClient);
  }

  /**
   * Uses reflection to set a static override field used for tests (e.g., httpClientOverride)
   *
   * @deprecated Will be removed when override fields are removed from BaseEppoClient
   */
  @Deprecated
  @SuppressWarnings("SameParameterValue")
  public static <T> void setBaseClientOverrideField(String fieldName, T override) {
    try {
      Field httpClientOverrideField = BaseEppoClient.class.getDeclaredField(fieldName);
      httpClientOverrideField.setAccessible(true);
      httpClientOverrideField.set(null, override);
      httpClientOverrideField.setAccessible(false);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}

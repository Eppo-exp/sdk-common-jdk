package cloud.eppo.helpers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import cloud.eppo.BaseEppoClient;
import cloud.eppo.EppoHttpClient;
import cloud.eppo.EppoHttpResponse;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import okhttp3.*;

public class TestUtils {

  @SuppressWarnings("SameParameterValue")
  public static EppoHttpClient mockHttpResponse(String responseBody) {
    // Create a mock instance of EppoHttpClient
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class, withSettings().lenient());

    // Mock sync get - return EppoHttpResponse
    EppoHttpResponse response = new EppoHttpResponse(responseBody.getBytes(), 200, null);
    when(mockHttpClient.get(anyString())).thenReturn(response);
    when(mockHttpClient.get(anyString(), any())).thenReturn(response);

    // Mock async get - return CompletableFuture<EppoHttpResponse>
    CompletableFuture<EppoHttpResponse> mockAsyncResponse = CompletableFuture.completedFuture(response);
    when(mockHttpClient.getAsync(anyString())).thenReturn(mockAsyncResponse);
    when(mockHttpClient.getAsync(anyString(), any())).thenReturn(mockAsyncResponse);

    setBaseClientHttpClientOverrideField(mockHttpClient);
    return mockHttpClient;
  }

  public static void mockHttpError() {
    // Create a mock instance of EppoHttpClient
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class, withSettings().lenient());

    // Mock sync get
    when(mockHttpClient.get(anyString())).thenThrow(new RuntimeException("Intentional Error"));
    when(mockHttpClient.get(anyString(), any())).thenThrow(new RuntimeException("Intentional Error"));

    // Mock async get
    CompletableFuture<EppoHttpResponse> mockAsyncResponse = new CompletableFuture<>();
    mockAsyncResponse.completeExceptionally(new RuntimeException("Intentional Error"));
    when(mockHttpClient.getAsync(anyString())).thenReturn(mockAsyncResponse);
    when(mockHttpClient.getAsync(anyString(), any())).thenReturn(mockAsyncResponse);

    setBaseClientHttpClientOverrideField(mockHttpClient);
  }

  public static void setBaseClientHttpClientOverrideField(EppoHttpClient httpClient) {
    setBaseClientOverrideField("httpClientOverride", httpClient);
  }

  /** Uses reflection to set a static override field used for tests (e.g., httpClientOverride) */
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

package cloud.eppo.helpers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cloud.eppo.BaseEppoClient;
import cloud.eppo.api.IFlagConfigResponse;
import cloud.eppo.api.configuration.ConfigurationResponse;
import cloud.eppo.api.configuration.IEppoConfigurationHttpClient;
import cloud.eppo.ufc.dto.FlagConfigResponse;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

public class TestUtils {

  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(EppoModule.eppoModule());

  @SuppressWarnings("SameParameterValue")
  public static IEppoConfigurationHttpClient mockHttpResponse(String responseBody) {
    // Create a mock instance of IEppoConfigurationHttpClient
    IEppoConfigurationHttpClient mockHttpClient = mock(IEppoConfigurationHttpClient.class);

    try {
      IFlagConfigResponse flagResponse = mapper.readValue(responseBody, FlagConfigResponse.class);
      CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> mockResponse =
          CompletableFuture.completedFuture(
              ConfigurationResponse.Flags.success(flagResponse, "test-etag"));
      when(mockHttpClient.fetchFlagConfiguration(any())).thenReturn(mockResponse);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse mock response", e);
    }

    setBaseClientHttpClientOverrideField(mockHttpClient);
    return mockHttpClient;
  }

  public static void mockHttpError() {
    // Create a mock instance of IEppoConfigurationHttpClient
    IEppoConfigurationHttpClient mockHttpClient = mock(IEppoConfigurationHttpClient.class);

    CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> failedFuture =
        new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Intentional Error"));
    when(mockHttpClient.fetchFlagConfiguration(any())).thenReturn(failedFuture);

    setBaseClientHttpClientOverrideField(mockHttpClient);
  }

  public static void setBaseClientHttpClientOverrideField(
      IEppoConfigurationHttpClient httpClient) {
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

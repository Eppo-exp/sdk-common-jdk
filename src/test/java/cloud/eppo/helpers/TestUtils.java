package cloud.eppo.helpers;

import cloud.eppo.BaseEppoClient;
import cloud.eppo.MockHttpClient;
import cloud.eppo.api.Callback;
import cloud.eppo.api.IHttpClient;
import cloud.eppo.exception.FetchException;
import java.lang.reflect.Field;

public class TestUtils {

  @SuppressWarnings("SameParameterValue")
  public static IHttpClient mockHttpResponse(String responseBody) {
    // Create a mock HTTP client with a test base URL
    MockHttpClient mockHttpClient = new MockHttpClient("http://test.eppo.cloud");

    // Mock both endpoints with the same response
    mockHttpClient.mockResponse("/flag-config/v1/config", responseBody);
    mockHttpClient.mockResponse("/flag-config/v1/bandits", responseBody);

    setBaseClientHttpClientOverrideField(mockHttpClient);
    return mockHttpClient;
  }

  public static void mockHttpError() {
    // Create a mock HTTP client that throws errors
    IHttpClient errorHttpClient =
        new IHttpClient() {
          @Override
          public byte[] fetch(String endpoint, java.util.Map<String, String> queryParams)
              throws FetchException {
            throw new FetchException("Intentional Error", new RuntimeException("Intentional Error"));
          }

          @Override
          public void fetchAsync(
              String endpoint,
              java.util.Map<String, String> queryParams,
              Callback<byte[]> callback) {
            callback.onError(new RuntimeException("Intentional Error"));
          }

          @Override
          public String getBaseUrl() {
            return "http://error.test.com";
          }
        };

    setBaseClientHttpClientOverrideField(errorHttpClient);
  }

  public static void setBaseClientHttpClientOverrideField(IHttpClient httpClient) {
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

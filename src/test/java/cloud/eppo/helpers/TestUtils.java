package cloud.eppo.helpers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import cloud.eppo.BaseEppoClient;
import cloud.eppo.EppoHttpClient;
import java.io.IOException;
import java.lang.reflect.Field;
import okhttp3.*;

public class TestUtils {

  @SuppressWarnings("SameParameterValue")
  public static void mockHttpResponse(String host, String responseBody) {
    // Create a mock instance of EppoHttpClient
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    // Mock sync get
    Response dummyResponse =
        new Response.Builder()
            // Used by test
            .code(200)
            .body(ResponseBody.create(responseBody, MediaType.get("application/json")))
            // Below properties are required to build the Response (but unused)
            .request(new Request.Builder().url(host).build())
            .protocol(Protocol.HTTP_1_1)
            .message("OK")
            .build();
    try {
      when(mockHttpClient.get(anyString())).thenReturn(dummyResponse.body().bytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Mock async get
    doAnswer(
            invocation -> {
              EppoHttpClient.RequestCallback callback = invocation.getArgument(1);
              callback.onSuccess(responseBody.getBytes());
              return null; // doAnswer doesn't require a return value
            })
        .when(mockHttpClient)
        .get(anyString(), any(EppoHttpClient.RequestCallback.class));

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

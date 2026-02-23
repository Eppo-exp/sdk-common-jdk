package cloud.eppo.helpers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cloud.eppo.http.EppoConfigurationClient;
import cloud.eppo.http.EppoConfigurationRequest;
import cloud.eppo.http.EppoConfigurationResponse;
import java.net.HttpURLConnection;
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
        EppoConfigurationResponse.success(
            HttpURLConnection.HTTP_OK, "test-version", responseBody);

    when(mockClient.execute(any(EppoConfigurationRequest.class)))
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

    when(mockClient.execute(any(EppoConfigurationRequest.class))).thenReturn(failedFuture);

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
        EppoConfigurationResponse.error(
            HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal Server Error".getBytes());

    when(mockClient.execute(any(EppoConfigurationRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(errorResponse));

    return mockClient;
  }
}

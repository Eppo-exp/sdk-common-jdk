package cloud.eppo.api;

import cloud.eppo.exception.FetchException;
import java.util.Map;

/**
 * Interface for making HTTP requests to fetch Eppo configuration data. Downstream SDKs can provide
 * custom implementations using their preferred HTTP library (OkHttp, Ktor, URLSession, Fetch API,
 * etc.).
 *
 * <p>Threading: Implementations control when and on which thread callbacks are invoked. No thread
 * guarantees are made by this interface.
 */
public interface IHttpClient {
  /**
   * Synchronously fetch data from an endpoint.
   *
   * @param endpoint The API endpoint path (e.g., "/flag-config/v1/config")
   * @param queryParams Query parameters to include in the request
   * @return Raw response bytes
   * @throws FetchException If the request fails
   */
  byte[] fetch(String endpoint, Map<String, String> queryParams) throws FetchException;

  /**
   * Asynchronously fetch data from an endpoint.
   *
   * @param endpoint The API endpoint path
   * @param queryParams Query parameters to include in the request
   * @param callback Callback to receive the result or error
   */
  void fetchAsync(String endpoint, Map<String, String> queryParams, Callback<byte[]> callback);

  /**
   * Get the base URL for this HTTP client.
   *
   * @return The base URL (e.g., "https://fscdn.eppo.cloud/api")
   */
  String getBaseUrl();
}

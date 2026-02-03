package cloud.eppo.helpers;

import cloud.eppo.http.EppoHttpClient;
import cloud.eppo.http.EppoHttpException;
import cloud.eppo.http.EppoHttpRequest;
import cloud.eppo.http.EppoHttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Test implementation of EppoHttpClient that returns pre-configured responses. */
public class TestHttpClient implements EppoHttpClient {

  private final Map<String, byte[]> responses = new HashMap<>();
  private Exception errorToThrow = null;

  /**
   * Configures a response for a given path.
   *
   * @param path the request path (e.g., "/flag-config/v1/config")
   * @param responseBody the response body bytes
   */
  public void setResponse(String path, byte[] responseBody) {
    responses.put(path, responseBody);
  }

  /**
   * Configures a response for a given path.
   *
   * @param path the request path
   * @param responseBody the response body as a string
   */
  public void setResponse(String path, String responseBody) {
    setResponse(path, responseBody.getBytes());
  }

  /**
   * Configures the client to throw an error on any request.
   *
   * @param error the error to throw
   */
  public void setError(Exception error) {
    this.errorToThrow = error;
  }

  @Override
  public CompletableFuture<EppoHttpResponse> get(EppoHttpRequest request) {
    CompletableFuture<EppoHttpResponse> future = new CompletableFuture<>();

    if (errorToThrow != null) {
      future.completeExceptionally(errorToThrow);
      return future;
    }

    String url = request.getUrl();
    byte[] responseBody = findResponseForUrl(url);

    if (responseBody != null) {
      future.complete(EppoHttpResponse.success(200, null, responseBody));
    } else {
      future.completeExceptionally(
          EppoHttpException.httpError(404, "No response configured for URL: " + url));
    }

    return future;
  }

  /**
   * Finds a response for the given URL by matching against stored paths. Supports both exact path
   * matches and suffix matches (e.g., path "/flag-config/v1/config" matches URL
   * "https://example.com/flag-config/v1/config").
   */
  private byte[] findResponseForUrl(String url) {
    // Try exact match first
    byte[] response = responses.get(url);
    if (response != null) {
      return response;
    }

    // Try suffix match for paths
    for (Map.Entry<String, byte[]> entry : responses.entrySet()) {
      String path = entry.getKey();
      if (url.endsWith(path)) {
        return entry.getValue();
      }
    }

    return null;
  }
}

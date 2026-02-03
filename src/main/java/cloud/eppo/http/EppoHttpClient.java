package cloud.eppo.http;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for HTTP client implementations used by the Eppo SDK.
 *
 * <p>Implementations of this interface handle all HTTP communication with Eppo's servers. The SDK
 * provides a default implementation using OkHttp in the eppo-sdk-common module, but custom
 * implementations can be provided for specialized use cases.
 */
public interface EppoHttpClient {

  /**
   * Performs an asynchronous GET request.
   *
   * <p>For synchronous behavior, callers can use {@code .get()} or {@code .join()} on the returned
   * CompletableFuture.
   *
   * @param request the request to execute
   * @return a CompletableFuture that will complete with the response
   */
  CompletableFuture<EppoConfigurationResponse> get(EppoConfigurationRequest request);
}

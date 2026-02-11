package cloud.eppo.http;

import java.util.concurrent.CompletableFuture;

/**
 * Defines the contract for configuration clients utilized by the Eppo SDK.
 *
 * <p>
 * Implementations of this interface are responsible for all interactions with configuration endpoint servers,
 * whether hosted by Eppo or others. The SDK includes a default implementation using OKHttp (in the eppo-sdk-common module),
 * but users can supply custom implementations to accommodate specialized needs, such as custom logging, 
 * authentication, or networking requirements.
 */
public interface EppoConfigurationClient {

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

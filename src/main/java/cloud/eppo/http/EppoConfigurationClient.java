package cloud.eppo.http;

import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the contract for configuration clients utilized by the Eppo SDK.
 *
 * <p>Implementations of this interface are responsible for all interactions with configuration
 * endpoint servers, whether hosted by Eppo or others. The SDK includes a default implementation
 * using OKHttp (in the eppo-sdk-common module), but users can supply custom implementations to
 * accommodate specialized needs, such as custom logging, authentication, or networking
 * requirements.
 */
public interface EppoConfigurationClient {

  /**
   * Executes a configuration request asynchronously.
   *
   * <p>The request may be either GET or POST based on {@link EppoConfigurationRequest#getMethod()}.
   * For POST requests, implementations should include the body and content type from the request.
   *
   * <p>For synchronous behavior, callers can use {@code .get()} or {@code .join()} on the returned
   * CompletableFuture.
   *
   * @param request the request to execute
   * @return a CompletableFuture that will complete with the response
   */
  @NotNull CompletableFuture<EppoConfigurationResponse> execute(@NotNull EppoConfigurationRequest request);

  /**
   * Performs an asynchronous GET request.
   *
   * @param request the request to execute
   * @return a CompletableFuture that will complete with the response
   * @deprecated Use {@link #execute(EppoConfigurationRequest)} instead. This method is maintained
   *     for backward compatibility.
   */
  @Deprecated
  @NotNull default CompletableFuture<EppoConfigurationResponse> get(
      @NotNull EppoConfigurationRequest request) {
    return execute(request);
  }
}

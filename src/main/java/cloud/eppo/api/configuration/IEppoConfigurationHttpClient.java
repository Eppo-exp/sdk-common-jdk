package cloud.eppo.api.configuration;

import cloud.eppo.api.IBanditParametersResponse;
import cloud.eppo.api.IFlagConfigResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for fetching Eppo configuration via HTTP.
 *
 * <p>This interface defines the contract for HTTP clients that fetch flag and bandit configuration
 * from Eppo's servers. Implementations handle the HTTP communication, JSON parsing, and ETag-based
 * caching.
 *
 * <p>Both synchronous and asynchronous methods are provided for each configuration type. The
 * asynchronous methods return {@link CompletableFuture} for non-blocking operations.
 *
 * <p><strong>Usage:</strong> Custom implementations can be provided to:
 *
 * <ul>
 *   <li>Use platform-specific HTTP clients (e.g., Android's HttpUrlConnection, Ktor)
 *   <li>Add middleware for logging, metrics, or tracing
 *   <li>Implement custom retry logic or circuit breakers
 *   <li>Add custom headers or authentication schemes
 * </ul>
 *
 * <p><strong>Default Implementation:</strong> {@code DefaultEppoConfigurationHttpClient} provides a
 * production-ready implementation using OkHttp and Jackson for JSON parsing.
 *
 * @see ConfigurationRequest
 * @see ConfigurationResponse
 */
public interface IEppoConfigurationHttpClient {

  /**
   * Synchronously fetches flag configuration from Eppo's servers.
   *
   * @param request the configuration request containing URL, API key, SDK metadata, and optional
   *     previous ETag for conditional requests
   * @return a {@link ConfigurationResponse} containing the parsed flag configuration (on success),
   *     ETag for caching, and HTTP status code. The response can be in one of three states:
   *     <ul>
   *       <li><strong>Success (200):</strong> {@code payload} contains parsed flags, {@code eTag}
   *           is set
   *       <li><strong>Not Modified (304):</strong> {@code payload} is null (use cached data),
   *           {@code eTag} is set
   *       <li><strong>Error (4xx/5xx):</strong> {@code payload} and {@code eTag} are null, {@code
   *           errorMessage} is set
   *     </ul>
   */
  <T extends IFlagConfigResponse> ConfigurationResponse<IFlagConfigResponse> fetchFlagConfiguration(
      ConfigurationRequest request);

  /**
   * Asynchronously fetches flag configuration from Eppo's servers.
   *
   * @param request the configuration request containing URL, API key, SDK metadata, and optional
   *     previous ETag for conditional requests
   * @return a {@link CompletableFuture} that completes with a {@link ConfigurationResponse}
   *     containing the parsed flag configuration (on success), ETag for caching, and HTTP status
   *     code. The response can be in one of three states:
   *     <ul>
   *       <li><strong>Success (200):</strong> {@code payload} contains parsed flags, {@code eTag}
   *           is set
   *       <li><strong>Not Modified (304):</strong> {@code payload} is null (use cached data),
   *           {@code eTag} is set
   *       <li><strong>Error (4xx/5xx):</strong> {@code payload} and {@code eTag} are null, {@code
   *           errorMessage} is set
   *     </ul>
   */
  <T extends IFlagConfigResponse>
      CompletableFuture<ConfigurationResponse<IFlagConfigResponse>> fetchFlagConfigurationAsync(
          ConfigurationRequest request);

  /**
   * Synchronously fetches bandit configuration from Eppo's servers.
   *
   * @param request the configuration request containing URL, API key, SDK metadata, and optional
   *     previous ETag for conditional requests
   * @return a {@link ConfigurationResponse} containing the parsed bandit parameters (on success),
   *     ETag for caching, and HTTP status code. The response can be in one of three states:
   *     <ul>
   *       <li><strong>Success (200):</strong> {@code payload} contains parsed bandit parameters,
   *           {@code eTag} is set
   *       <li><strong>Not Modified (304):</strong> {@code payload} is null (use cached data),
   *           {@code eTag} is set
   *       <li><strong>Error (4xx/5xx):</strong> {@code payload} and {@code eTag} are null, {@code
   *           errorMessage} is set
   *     </ul>
   */
  <T extends IBanditParametersResponse>
      ConfigurationResponse<IBanditParametersResponse> fetchBanditConfiguration(
          ConfigurationRequest request);

  /**
   * Asynchronously fetches bandit configuration from Eppo's servers.
   *
   * @param request the configuration request containing URL, API key, SDK metadata, and optional
   *     previous ETag for conditional requests
   * @return a {@link CompletableFuture} that completes with a {@link ConfigurationResponse}
   *     containing the parsed bandit parameters (on success), ETag for caching, and HTTP status
   *     code. The response can be in one of three states:
   *     <ul>
   *       <li><strong>Success (200):</strong> {@code payload} contains parsed bandit parameters,
   *           {@code eTag} is set
   *       <li><strong>Not Modified (304):</strong> {@code payload} is null (use cached data),
   *           {@code eTag} is set
   *       <li><strong>Error (4xx/5xx):</strong> {@code payload} and {@code eTag} are null, {@code
   *           errorMessage} is set
   *     </ul>
   */
  <T extends IBanditParametersResponse>
      CompletableFuture<ConfigurationResponse<IBanditParametersResponse>>
          fetchBanditConfigurationAsync(ConfigurationRequest request);
}

package cloud.eppo.api.configuration;

import cloud.eppo.api.IBanditParametersResponse;
import cloud.eppo.api.IFlagConfigResponse;

/**
 * Immutable HTTP response wrapper for configuration fetches.
 *
 * <p>This generic class wraps the HTTP response from Eppo's configuration API, including the parsed
 * payload, ETag for caching, HTTP status code, and any error messages.
 *
 * <p><strong>Generic Type:</strong> {@code PayloadType} is either {@link IFlagConfigResponse} for
 * flag configuration or {@link IBanditParametersResponse} for bandit parameters.
 *
 * <p><strong>Response States:</strong> A response can be in one of three states:
 *
 * <ul>
 *   <li><strong>Success (200):</strong> {@code payload} contains the parsed configuration, {@code
 *       eTag} is set for future caching
 *   <li><strong>Not Modified (304):</strong> {@code payload} is null (use previously cached data),
 *       {@code eTag} is set
 *   <li><strong>Error (4xx/5xx):</strong> {@code payload} and {@code eTag} are null, {@code
 *       errorMessage} describes the failure
 * </ul>
 *
 * <p><strong>Factory Methods:</strong> Use the static factory methods in {@link Flags} or {@link
 * Bandits} inner classes instead of calling constructors directly. This ensures type safety and
 * proper initialization.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Success response
 * ConfigurationResponse<IFlagConfigResponse> response =
 *     ConfigurationResponse.Flags.success(flagConfig, "etag-12345");
 *
 * // Not modified response
 * ConfigurationResponse<IFlagConfigResponse> cached =
 *     ConfigurationResponse.Flags.notModified("etag-12345");
 *
 * // Error response
 * ConfigurationResponse<IFlagConfigResponse> error =
 *     ConfigurationResponse.Flags.error(404, "Configuration not found");
 * }</pre>
 *
 * @param <PayloadType> the type of configuration payload (IFlagConfigResponse or
 *     IBanditParametersResponse)
 * @see IEppoConfigurationHttpClient
 * @see ConfigurationRequest
 */
public class ConfigurationResponse<PayloadType> {
  private final PayloadType payload;
  private final String eTag;
  private final int statusCode;
  private final String errorMessage;

  private ConfigurationResponse(
      PayloadType payload, String eTag, int statusCode, String errorMessage) {
    this.payload = payload;
    this.eTag = eTag;
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
  }

  private ConfigurationResponse(PayloadType payload, String eTag, int statusCode) {
    this.payload = payload;
    this.eTag = eTag;
    this.statusCode = statusCode;
    this.errorMessage = null;
  }

  /**
   * Returns the parsed configuration payload.
   *
   * @return the parsed configuration payload for successful (200) responses, or null for 304 Not
   *     Modified or error responses
   */
  public PayloadType getPayload() {
    return payload;
  }

  /**
   * Returns the HTTP ETag header value for caching.
   *
   * <p>The ETag can be provided in subsequent requests via {@link
   * ConfigurationRequest#getPreviousETag()} to enable conditional requests. If the configuration
   * hasn't changed, the server responds with 304 Not Modified.
   *
   * @return the ETag value for successful (200) or not modified (304) responses, or null for error
   *     responses
   */
  public String getETag() {
    return eTag;
  }

  /**
   * Returns the HTTP status code.
   *
   * @return the HTTP status code (200 for success, 304 for not modified, or 4xx/5xx for errors)
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Returns the error message for failed requests.
   *
   * @return a descriptive error message for failed requests, or null for successful (200) or not
   *     modified (304) responses
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Factory methods for creating flag configuration responses.
   *
   * <p>Use these static methods to create type-safe responses for flag configuration fetches.
   */
  public static class Flags {
    /**
     * Creates a "Not Modified" response (HTTP 304).
     *
     * <p>Indicates the configuration hasn't changed since the previous request. The client should
     * use its cached configuration data.
     *
     * @param eTag the ETag from the server indicating the current configuration version
     * @return a ConfigurationResponse with status 304 and null payload
     */
    public static ConfigurationResponse<IFlagConfigResponse> notModified(String eTag) {
      return new ConfigurationResponse<>(null, eTag, 304);
    }

    /**
     * Creates a successful response (HTTP 200) with flag configuration.
     *
     * @param payload the parsed flag configuration
     * @param eTag the ETag for caching this configuration version
     * @return a ConfigurationResponse with status 200 and the parsed configuration
     */
    public static ConfigurationResponse<IFlagConfigResponse> success(
        IFlagConfigResponse payload, String eTag) {
      return new ConfigurationResponse<>(payload, eTag, 200);
    }

    /**
     * Creates an error response (HTTP 4xx/5xx).
     *
     * @param statusCode the HTTP error status code (e.g., 404, 500)
     * @param errorMessage a descriptive error message
     * @return a ConfigurationResponse with the error status and message
     */
    public static ConfigurationResponse<IFlagConfigResponse> error(
        int statusCode, String errorMessage) {
      return new ConfigurationResponse<>(null, null, statusCode, errorMessage);
    }
  }

  /**
   * Factory methods for creating bandit configuration responses.
   *
   * <p>Use these static methods to create type-safe responses for bandit parameter fetches.
   */
  public static class Bandits {
    /**
     * Creates a "Not Modified" response (HTTP 304).
     *
     * <p>Indicates the bandit parameters haven't changed since the previous request. The client
     * should use its cached data.
     *
     * @param eTag the ETag from the server indicating the current configuration version
     * @return a ConfigurationResponse with status 304 and null payload
     */
    public static ConfigurationResponse<IBanditParametersResponse> notModified(String eTag) {
      return new ConfigurationResponse<>(null, eTag, 304);
    }

    /**
     * Creates a successful response (HTTP 200) with bandit parameters.
     *
     * @param payload the parsed bandit parameters
     * @param eTag the ETag for caching this configuration version
     * @return a ConfigurationResponse with status 200 and the parsed parameters
     */
    public static ConfigurationResponse<IBanditParametersResponse> success(
        IBanditParametersResponse payload, String eTag) {
      return new ConfigurationResponse<>(payload, eTag, 200);
    }

    /**
     * Creates an error response (HTTP 4xx/5xx).
     *
     * @param statusCode the HTTP error status code (e.g., 404, 500)
     * @param errorMessage a descriptive error message
     * @return a ConfigurationResponse with the error status and message
     */
    public static ConfigurationResponse<IBanditParametersResponse> error(
        int statusCode, String errorMessage) {
      return new ConfigurationResponse<>(null, null, statusCode, errorMessage);
    }
  }

  /**
   * Checks if this is a "Not Modified" response.
   *
   * @return true if the status code is 304 (Not Modified), false otherwise
   */
  public boolean isNotModified() {
    return statusCode == 304;
  }

  /**
   * Checks if this is a successful response.
   *
   * @return true if the status code is 200 (OK), false otherwise
   */
  public boolean isSuccess() {
    return statusCode == 200;
  }

  /**
   * Checks if this is an error response.
   *
   * @return true if the status code indicates an error (not 200 or 304), false otherwise
   */
  public boolean isError() {
    return statusCode != 200 && statusCode != 304;
  }
}

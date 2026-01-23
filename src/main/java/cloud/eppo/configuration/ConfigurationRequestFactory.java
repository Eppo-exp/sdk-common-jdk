package cloud.eppo.configuration;

import cloud.eppo.api.configuration.ConfigurationRequest;

/**
 * Factory for creating {@link ConfigurationRequest} objects with SDK context.
 *
 * <p>This factory encapsulates the SDK's base URL, API key, and metadata (SDK name and version)
 * that remain constant across multiple configuration requests. It provides a convenient way to
 * create request objects with only the variable ETag parameter.
 *
 * <p><strong>Usage:</strong> Typically instantiated once per client initialization with the SDK's
 * configuration, then used to create individual requests:
 *
 * <pre>{@code
 * ConfigurationRequestFactory factory = new ConfigurationRequestFactory(
 *     "https://api.eppo.cloud/api",
 *     "my-api-key",
 *     "java-server-sdk",
 *     "4.0.0"
 * );
 *
 * // First request (no ETag)
 * ConfigurationRequest firstRequest = factory.createConfigurationRequest(null);
 *
 * // Subsequent request with ETag for caching
 * ConfigurationRequest cachedRequest = factory.createConfigurationRequest("etag-12345");
 * }</pre>
 *
 * @see ConfigurationRequest
 * @see cloud.eppo.api.configuration.IEppoConfigurationHttpClient
 */
public class ConfigurationRequestFactory {
  private String url;
  private String apiKey;
  private String sdkName;
  private String sdkVersion;

  /**
   * Constructs a new configuration request factory with SDK context.
   *
   * @param url the base URL for Eppo's API (e.g., "https://api.eppo.cloud/api")
   * @param apiKey the Eppo API key for authentication
   * @param sdkName the name of the SDK (e.g., "java-server-sdk")
   * @param sdkVersion the version of the SDK (e.g., "4.0.0")
   */
  public ConfigurationRequestFactory(String url, String apiKey, String sdkName, String sdkVersion) {
    this.url = url;
    this.apiKey = apiKey;
    this.sdkName = sdkName;
    this.sdkVersion = sdkVersion;
  }

  /**
   * Creates a configuration request with the factory's SDK context.
   *
   * <p>This method combines the factory's URL, API key, and SDK metadata with the provided ETag to
   * create a complete {@link ConfigurationRequest}.
   *
   * @param previousETag the ETag from a previous response for conditional requests, or null for
   *     unconditional requests. When provided, enables 304 Not Modified responses.
   * @return a new ConfigurationRequest with all required parameters
   */
  public ConfigurationRequest createConfigurationRequest(String previousETag) {
    return new ConfigurationRequest(url, apiKey, sdkName, sdkVersion, previousETag);
  }
}

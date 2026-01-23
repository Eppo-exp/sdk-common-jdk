package cloud.eppo.api.configuration;

/**
 * Immutable request object for fetching Eppo configuration via HTTP.
 *
 * <p>This class encapsulates all the information needed to make an HTTP request to Eppo's
 * configuration API, including authentication credentials, SDK metadata, and optional caching
 * headers.
 *
 * <p><strong>Fields:</strong>
 *
 * <ul>
 *   <li><strong>url:</strong> The complete endpoint URL for the configuration API
 *   <li><strong>apiKey:</strong> Eppo API key for authentication
 *   <li><strong>sdkName:</strong> Name of the SDK making the request (e.g., "java-server-sdk")
 *   <li><strong>sdkVersion:</strong> Version of the SDK making the request (e.g., "4.0.0")
 *   <li><strong>previousETag:</strong> Optional ETag from a previous response for conditional
 *       requests
 * </ul>
 *
 * <p><strong>ETag Usage:</strong> When {@code previousETag} is provided, it enables HTTP
 * conditional requests using the {@code If-None-Match} header. If the server's configuration hasn't
 * changed, it responds with 304 Not Modified, saving bandwidth and processing time.
 *
 * @see IEppoConfigurationHttpClient
 * @see ConfigurationResponse
 */
public class ConfigurationRequest {
  private final String url;
  private final String apiKey;
  private final String sdkName;
  private final String sdkVersion;
  private final String previousETag;

  /**
   * Constructs a new configuration request.
   *
   * @param url the complete endpoint URL for the configuration API (e.g.,
   *     "https://api.eppo.cloud/api/flag-config/v1/config")
   * @param apiKey the Eppo API key for authentication
   * @param sdkName the name of the SDK making the request (e.g., "java-server-sdk")
   * @param sdkVersion the version of the SDK making the request (e.g., "4.0.0")
   * @param previousETag the ETag from a previous response, or null for unconditional requests. When
   *     provided, enables 304 Not Modified responses.
   */
  public ConfigurationRequest(
      String url, String apiKey, String sdkName, String sdkVersion, String previousETag) {
    this.url = url;
    this.apiKey = apiKey;
    this.sdkName = sdkName;
    this.sdkVersion = sdkVersion;
    this.previousETag = previousETag;
  }

  /** Returns the complete endpoint URL for the configuration API. */
  public String getUrl() {
    return url;
  }

  /** Returns the Eppo API key for authentication. */
  public String getApiKey() {
    return apiKey;
  }

  /** Returns the name of the SDK making the request. */
  public String getSdkName() {
    return sdkName;
  }

  /** Returns the version of the SDK making the request. */
  public String getSdkVersion() {
    return sdkVersion;
  }

  /**
   * Returns the previous ETag for conditional requests.
   *
   * <p>If provided, this enables HTTP conditional requests using the {@code If-None-Match} header.
   * When the server's configuration matches this ETag, it responds with 304 Not Modified instead of
   * re-sending the full configuration payload.
   *
   * @return the previous ETag, or null for unconditional requests
   */
  public String getPreviousETag() {
    return previousETag;
  }
}

package cloud.eppo.http;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a configuration request to be executed by an {@link EppoConfigurationClient}.
 *
 * <p>This class is immutable. HTTP client implementations are responsible for combining the base
 * URL, resource path, and query parameters using their library's URL building utilities.
 *
 * <p>Requests can be created using either the constructor (for backward compatibility with GET
 * requests) or the {@link Builder} for more control including POST requests with body.
 */
public final class EppoConfigurationRequest {

  /** HTTP methods supported by configuration requests. */
  public enum HttpMethod {
    GET,
    POST
  }

  private final String baseUrl;
  private final String resourcePath;
  private final Map<String, String> queryParams;
  @Nullable private final String lastVersionId;
  @NotNull private final HttpMethod method;
  @Nullable private final byte[] body;
  @Nullable private final String contentType;

  /**
   * Creates a new GET configuration request.
   *
   * <p>This constructor is maintained for backward compatibility. For POST requests or more control
   * over request parameters, use the {@link Builder}.
   *
   * @param baseUrl the base URL (e.g., "https://fscdn.eppo.cloud")
   * @param resourcePath the resource path (e.g., "/api/flag-config/v1/config")
   * @param queryParams query parameters to append to the URL
   * @param lastVersionId the last known version ID for conditional requests, or null
   */
  public EppoConfigurationRequest(
      @NotNull String baseUrl,
      @NotNull String resourcePath,
      @NotNull Map<String, String> queryParams,
      @Nullable String lastVersionId) {
    this.baseUrl = baseUrl;
    this.resourcePath = resourcePath;
    this.queryParams = Collections.unmodifiableMap(new LinkedHashMap<>(queryParams));
    this.lastVersionId = lastVersionId;
    this.method = HttpMethod.GET;
    this.body = null;
    this.contentType = null;
  }

  private EppoConfigurationRequest(Builder builder) {
    this.baseUrl = builder.baseUrl;
    this.resourcePath = builder.resourcePath;
    this.queryParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.queryParams));
    this.lastVersionId = builder.lastVersionId;
    this.method = builder.method;
    this.body = builder.body;
    this.contentType = builder.contentType;
  }

  /**
   * Returns the base URL for the request.
   *
   * @return the base URL (e.g., "https://fscdn.eppo.cloud")
   */
  @NotNull
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Returns the resource path for the request.
   *
   * @return the resource path (e.g., "/api/flag-config/v1/config")
   */
  @NotNull
  public String getResourcePath() {
    return resourcePath;
  }

  /**
   * Returns the query parameters to append to the URL.
   *
   * @return an unmodifiable map of query parameters
   */
  @NotNull
  public Map<String, String> getQueryParams() {
    return queryParams;
  }

  /**
   * Returns the last known version identifier for conditional requests.
   *
   * <p>When set, the HTTP client should include an "If-None-Match" header with this value. If the
   * server's current version matches, a 304 Not Modified response will be returned.
   *
   * @return the last version ID, or null if not set
   */
  @Nullable
  public String getLastVersionId() {
    return lastVersionId;
  }

  /**
   * Returns the HTTP method for this request.
   *
   * @return the HTTP method (GET or POST)
   */
  @NotNull
  public HttpMethod getMethod() {
    return method;
  }

  /**
   * Returns the request body for POST requests.
   *
   * @return the body bytes, or null if not set
   */
  @Nullable
  public byte[] getBody() {
    return body;
  }

  /**
   * Returns the content type for the request body.
   *
   * @return the content type (e.g., "application/json; charset=utf-8"), or null if not set
   */
  @Nullable
  public String getContentType() {
    return contentType;
  }

  /**
   * Factory method for creating GET requests (backward compatibility).
   *
   * @param baseUrl the base URL
   * @param resourcePath the resource path
   * @param queryParams query parameters to append to the URL
   * @param lastVersionId the last known version ID for conditional requests, or null
   * @return a new GET configuration request
   */
  @NotNull
  public static EppoConfigurationRequest get(
      @NotNull String baseUrl,
      @NotNull String resourcePath,
      @NotNull Map<String, String> queryParams,
      @Nullable String lastVersionId) {
    return new EppoConfigurationRequest(baseUrl, resourcePath, queryParams, lastVersionId);
  }

  /** Builder for creating configuration requests with full control over all parameters. */
  public static class Builder {
    private final String baseUrl;
    private final String resourcePath;
    private Map<String, String> queryParams = new LinkedHashMap<>();
    @Nullable private String lastVersionId;
    private HttpMethod method = HttpMethod.GET;
    @Nullable private byte[] body;
    @Nullable private String contentType;

    /**
     * Creates a new builder with required base URL and resource path.
     *
     * @param baseUrl the base URL (e.g., "https://fscdn.eppo.cloud")
     * @param resourcePath the resource path (e.g., "/api/flag-config/v1/config")
     */
    public Builder(@NotNull String baseUrl, @NotNull String resourcePath) {
      this.baseUrl = baseUrl;
      this.resourcePath = resourcePath;
    }

    /**
     * Sets all query parameters, replacing any previously set.
     *
     * @param queryParams the query parameters map
     * @return this builder
     */
    public Builder queryParams(@NotNull Map<String, String> queryParams) {
      this.queryParams = new LinkedHashMap<>(queryParams);
      return this;
    }

    /**
     * Adds a single query parameter.
     *
     * @param key the parameter name
     * @param value the parameter value
     * @return this builder
     */
    public Builder queryParam(@NotNull String key, @NotNull String value) {
      this.queryParams.put(key, value);
      return this;
    }

    /**
     * Sets the last version ID for conditional requests (If-None-Match header).
     *
     * @param lastVersionId the last known version ID, or null
     * @return this builder
     */
    public Builder lastVersionId(@Nullable String lastVersionId) {
      this.lastVersionId = lastVersionId;
      return this;
    }

    /**
     * Sets the HTTP method for this request.
     *
     * @param method the HTTP method
     * @return this builder
     */
    public Builder method(@NotNull HttpMethod method) {
      this.method = method;
      return this;
    }

    /**
     * Convenience method to set the HTTP method to POST.
     *
     * @return this builder
     */
    public Builder post() {
      this.method = HttpMethod.POST;
      return this;
    }

    /**
     * Sets the request body as raw bytes.
     *
     * @param body the body bytes, or null
     * @return this builder
     */
    public Builder body(@Nullable byte[] body) {
      this.body = body;
      return this;
    }

    /**
     * Sets the request body as a string (encoded as UTF-8).
     *
     * @param body the body string, or null
     * @return this builder
     */
    public Builder body(@Nullable String body) {
      this.body = body != null ? body.getBytes(StandardCharsets.UTF_8) : null;
      return this;
    }

    /**
     * Sets the content type for the request body.
     *
     * @param contentType the content type (e.g., "application/json"), or null
     * @return this builder
     */
    public Builder contentType(@Nullable String contentType) {
      this.contentType = contentType;
      return this;
    }

    /**
     * Sets a JSON body with the appropriate content type.
     *
     * @param body the JSON body as bytes, or null
     * @return this builder
     */
    public Builder jsonBody(@Nullable byte[] body) {
      this.body = body;
      this.contentType = "application/json; charset=utf-8";
      return this;
    }

    /**
     * Sets a JSON body with the appropriate content type.
     *
     * @param body the JSON body as a string, or null
     * @return this builder
     */
    public Builder jsonBody(@Nullable String body) {
      return jsonBody(body != null ? body.getBytes(StandardCharsets.UTF_8) : null);
    }

    /**
     * Builds the configuration request.
     *
     * @return a new immutable configuration request
     */
    public EppoConfigurationRequest build() {
      return new EppoConfigurationRequest(this);
    }
  }
}

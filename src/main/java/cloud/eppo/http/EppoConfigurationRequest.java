package cloud.eppo.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a configuration request to be executed by an {@link EppoHttpClient}.
 *
 * <p>This class is immutable and should be constructed using the {@link Builder}.
 */
public final class EppoConfigurationRequest {

  private final String uri;
  private final Map<String, String> queryParams;
  private final String lastVersionId;

  private EppoConfigurationRequest(Builder builder) {
    this.uri = builder.url;
    this.queryParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.queryParams));
    this.lastVersionId = builder.lastVersionId;
  }

  /**
   * Returns the base URL for the request.
   *
   * @return the URL
   */
  public String getUri() {
    return uri;
  }

  /**
   * Returns the query parameters to be appended to the URL.
   *
   * @return an unmodifiable map of query parameters
   */
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
  public String getLastVersionId() {
    return lastVersionId;
  }

  /**
   * Creates a new builder for constructing an EppoConfigurationRequest.
   *
   * @param url the base URL for the request
   * @return a new builder
   */
  public static Builder builder(String url) {
    return new Builder(url);
  }

  /** Builder for constructing {@link EppoConfigurationRequest} instances. */
  public static final class Builder {
    private final String url;
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private String lastVersionId;

    private Builder(String url) {
      if (url == null || url.isEmpty()) {
        throw new IllegalArgumentException("URL cannot be null or empty");
      }
      this.url = url;
    }

    /**
     * Adds a query parameter to the request.
     *
     * @param key the parameter name
     * @param value the parameter value
     * @return this builder
     */
    public Builder queryParam(String key, String value) {
      if (key != null && value != null) {
        queryParams.put(key, value);
      }
      return this;
    }

    /**
     * Sets the last known version identifier for conditional requests.
     *
     * <p>When set, the HTTP client should include an "If-None-Match" header with this value. If the
     * server's current version matches, a 304 Not Modified response will be returned.
     *
     * @param versionId the version ID from a previous response
     * @return this builder
     */
    public Builder ifNoneMatch(String versionId) {
      this.lastVersionId = versionId;
      return this;
    }

    /**
     * Builds the EppoConfigurationRequest.
     *
     * @return a new EppoConfigurationRequest instance
     */
    public EppoConfigurationRequest build() {
      return new EppoConfigurationRequest(this);
    }
  }
}

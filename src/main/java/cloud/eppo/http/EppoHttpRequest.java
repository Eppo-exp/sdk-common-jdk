package cloud.eppo.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an HTTP request to be executed by an {@link EppoHttpClient}.
 *
 * <p>This class is immutable and should be constructed using the {@link Builder}.
 */
public final class EppoHttpRequest {

  private final String url;
  private final Map<String, String> queryParams;
  private final String ifNoneMatchEtag;

  private EppoHttpRequest(Builder builder) {
    this.url = builder.url;
    this.queryParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.queryParams));
    this.ifNoneMatchEtag = builder.ifNoneMatchEtag;
  }

  /**
   * Returns the base URL for the request.
   *
   * @return the URL
   */
  public String getUrl() {
    return url;
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
   * Returns the ETag value for conditional GET requests (If-None-Match header).
   *
   * @return the ETag value, or null if not set
   */
  public String getIfNoneMatchEtag() {
    return ifNoneMatchEtag;
  }

  /**
   * Creates a new builder for constructing an EppoHttpRequest.
   *
   * @param url the base URL for the request
   * @return a new builder
   */
  public static Builder builder(String url) {
    return new Builder(url);
  }

  /** Builder for constructing {@link EppoHttpRequest} instances. */
  public static final class Builder {
    private final String url;
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private String ifNoneMatchEtag;

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
     * Sets the ETag value for conditional GET requests.
     *
     * <p>When set, the HTTP client should include an "If-None-Match" header with this value. If the
     * server returns a 304 Not Modified response, the client can skip downloading the response
     * body.
     *
     * @param etag the ETag value from a previous response
     * @return this builder
     */
    public Builder ifNoneMatch(String etag) {
      this.ifNoneMatchEtag = etag;
      return this;
    }

    /**
     * Builds the EppoHttpRequest.
     *
     * @return a new EppoHttpRequest instance
     */
    public EppoHttpRequest build() {
      return new EppoHttpRequest(this);
    }
  }
}

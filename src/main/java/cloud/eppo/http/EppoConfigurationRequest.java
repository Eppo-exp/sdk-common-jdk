package cloud.eppo.http;

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
 */
public final class EppoConfigurationRequest {

  private final String baseUrl;
  private final String resourcePath;
  private final Map<String, String> queryParams;
  private final String lastVersionId;

  /**
   * Creates a new configuration request.
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
  }

  /**
   * Returns the base URL for the request.
   *
   * @return the base URL (e.g., "https://fscdn.eppo.cloud")
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Returns the resource path for the request.
   *
   * @return the resource path (e.g., "/api/flag-config/v1/config")
   */
  public String getResourcePath() {
    return resourcePath;
  }

  /**
   * Returns the query parameters to append to the URL.
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
  @Nullable public String getLastVersionId() {
    return lastVersionId;
  }
}

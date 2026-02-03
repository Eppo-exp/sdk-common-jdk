package cloud.eppo.http;

/**
 * Represents a configuration response from an {@link EppoHttpClient}.
 *
 * <p>This class is immutable and provides factory methods for common response types.
 */
public final class EppoConfigurationResponse {

  private final int statusCode;
  private final String versionId;
  private final byte[] body;

  private EppoConfigurationResponse(int statusCode, String versionId, byte[] body) {
    this.statusCode = statusCode;
    this.versionId = versionId;
    this.body = body;
  }

  /**
   * Creates a successful response.
   *
   * @param statusCode the HTTP status code (2xx)
   * @param versionId the version identifier for this configuration, or null if not present
   * @param body the response body
   * @return a new successful response
   */
  public static EppoConfigurationResponse success(int statusCode, String versionId, byte[] body) {
    return new EppoConfigurationResponse(statusCode, versionId, body);
  }

  /**
   * Creates a 304 Not Modified response.
   *
   * @param versionId the version identifier, or null if not present
   * @return a new not modified response
   */
  public static EppoConfigurationResponse notModified(String versionId) {
    return new EppoConfigurationResponse(304, versionId, null);
  }

  /**
   * Creates an error response.
   *
   * @param statusCode the HTTP status code (4xx or 5xx)
   * @param body the error response body, or null
   * @return a new error response
   */
  public static EppoConfigurationResponse error(int statusCode, byte[] body) {
    return new EppoConfigurationResponse(statusCode, null, body);
  }

  /**
   * Returns the HTTP status code.
   *
   * @return the status code
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Returns the version identifier for this configuration.
   *
   * <p>This can be used in subsequent requests to enable conditional fetching (304 Not Modified).
   *
   * @return the version ID, or null if not present
   */
  public String getVersionId() {
    return versionId;
  }

  /**
   * Returns the response body.
   *
   * @return the body bytes, or null for 304 responses
   */
  public byte[] getBody() {
    return body;
  }

  /**
   * Checks if this is a 304 Not Modified response.
   *
   * @return true if the status code is 304
   */
  public boolean isNotModified() {
    return statusCode == 304;
  }

  /**
   * Checks if this is a successful response (2xx status code).
   *
   * @return true if the status code is between 200 and 299
   */
  public boolean isSuccessful() {
    return statusCode >= 200 && statusCode < 300;
  }
}

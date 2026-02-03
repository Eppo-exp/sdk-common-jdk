package cloud.eppo.http;

/**
 * Represents an HTTP response from an {@link EppoHttpClient}.
 *
 * <p>This class is immutable and provides factory methods for common response types.
 */
public final class EppoHttpResponse {

  private final int statusCode;
  private final String etag;
  private final byte[] body;

  private EppoHttpResponse(int statusCode, String etag, byte[] body) {
    this.statusCode = statusCode;
    this.etag = etag;
    this.body = body;
  }

  /**
   * Creates a successful response.
   *
   * @param statusCode the HTTP status code (2xx)
   * @param etag the ETag header value, or null if not present
   * @param body the response body
   * @return a new successful response
   */
  public static EppoHttpResponse success(int statusCode, String etag, byte[] body) {
    return new EppoHttpResponse(statusCode, etag, body);
  }

  /**
   * Creates a 304 Not Modified response.
   *
   * @param etag the ETag header value, or null if not present
   * @return a new not modified response
   */
  public static EppoHttpResponse notModified(String etag) {
    return new EppoHttpResponse(304, etag, null);
  }

  /**
   * Creates an error response.
   *
   * @param statusCode the HTTP status code (4xx or 5xx)
   * @param body the error response body, or null
   * @return a new error response
   */
  public static EppoHttpResponse error(int statusCode, byte[] body) {
    return new EppoHttpResponse(statusCode, null, body);
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
   * Returns the ETag header value.
   *
   * @return the ETag, or null if not present
   */
  public String getEtag() {
    return etag;
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

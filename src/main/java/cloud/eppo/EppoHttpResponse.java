package cloud.eppo;

import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for HTTP responses containing status code, body, and ETag for caching.
 *
 * <p>This class enables HTTP caching via ETags: - 200 OK: Contains response body and new ETag - 304
 * Not Modified: Empty body, current ETag retained
 */
public class EppoHttpResponse {
  private final byte[] body;
  private final int statusCode;
  @Nullable private final String eTag;

  public EppoHttpResponse(byte[] body, int statusCode, @Nullable String eTag) {
    this.body = body;
    this.statusCode = statusCode;
    this.eTag = eTag;
  }

  /**
   * Get response body bytes. Will be empty for 304 Not Modified responses.
   *
   * @return Response body as bytes
   */
  public byte[] getBody() {
    return body;
  }

  /**
   * Get HTTP status code.
   *
   * @return Status code (200, 304, 403, 500, etc.)
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Get ETag header value if present.
   *
   * @return ETag value or null if not present
   */
  @Nullable public String getETag() {
    return eTag;
  }

  /**
   * Returns true if status code is 304 Not Modified.
   *
   * @return true if configuration hasn't changed
   */
  public boolean isNotModified() {
    return statusCode == 304;
  }

  /**
   * Returns true if status code indicates success (2xx range).
   *
   * @return true if request was successful
   */
  public boolean isSuccessful() {
    return statusCode >= 200 && statusCode < 300;
  }

  @Override
  public String toString() {
    return "EppoHttpResponse{"
        + "statusCode="
        + statusCode
        + ", eTag='"
        + eTag
        + '\''
        + ", bodyLength="
        + body.length
        + '}';
  }
}

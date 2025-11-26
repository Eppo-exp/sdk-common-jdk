package cloud.eppo;

import org.jetbrains.annotations.Nullable;

/**
 * HTTP response wrapper containing status code, body, and optional ETag header. Used to support
 * conditional requests via If-None-Match/ETag headers.
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

  /** Get response body bytes. Empty for 304 responses. */
  public byte[] getBody() {
    return body;
  }

  /** Get HTTP status code. */
  public int getStatusCode() {
    return statusCode;
  }

  /** Get ETag header value if present. */
  @Nullable public String getETag() {
    return eTag;
  }

  /** Returns true if status code is 304 Not Modified. */
  public boolean isNotModified() {
    return statusCode == 304;
  }

  /** Returns true if status code is 2xx success. */
  public boolean isSuccessful() {
    return statusCode >= 200 && statusCode < 300;
  }
}

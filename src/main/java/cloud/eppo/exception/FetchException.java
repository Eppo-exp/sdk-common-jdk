package cloud.eppo.exception;

/**
 * Exception thrown when an HTTP fetch operation fails.
 */
public class FetchException extends Exception {
  private final int statusCode;
  private final String endpoint;

  /**
   * Constructor for HTTP errors with status code.
   *
   * @param message User-friendly error message
   * @param statusCode HTTP status code (e.g., 403, 404, 500)
   * @param endpoint The endpoint that was being requested
   */
  public FetchException(String message, int statusCode, String endpoint) {
    super(message);
    this.statusCode = statusCode;
    this.endpoint = endpoint;
  }

  /**
   * Constructor for network errors without status code.
   *
   * @param message User-friendly error message
   * @param cause The underlying cause of the failure
   */
  public FetchException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = -1;
    this.endpoint = null;
  }

  /**
   * Get the HTTP status code if available.
   *
   * @return HTTP status code, or -1 if this was a network error
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Get the endpoint that was being requested.
   *
   * @return The endpoint path, or null if not available
   */
  public String getEndpoint() {
    return endpoint;
  }

  /**
   * Check if this is an authentication error (HTTP 403).
   *
   * @return true if this was a 403 Forbidden error
   */
  public boolean isAuthError() {
    return statusCode == 403;
  }

  /**
   * Check if this is a network error (no HTTP status).
   *
   * @return true if this was a network-level failure
   */
  public boolean isNetworkError() {
    return statusCode < 0;
  }
}

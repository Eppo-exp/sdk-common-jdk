package cloud.eppo.http;

/**
 * Exception thrown when HTTP operations fail.
 *
 * <p>This exception provides factory methods for common HTTP error scenarios.
 */
public class EppoHttpException extends RuntimeException {

  private final int statusCode;
  private final boolean isUnauthorized;

  private EppoHttpException(String message, Throwable cause, int statusCode, boolean isUnauthorized) {
    super(message, cause);
    this.statusCode = statusCode;
    this.isUnauthorized = isUnauthorized;
  }

  /**
   * Creates an exception for unauthorized (401/403) responses.
   *
   * @return a new unauthorized exception
   */
  public static EppoHttpException unauthorized() {
    return new EppoHttpException("Invalid API key", null, 401, true);
  }

  /**
   * Creates an exception for network errors.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @return a new network error exception
   */
  public static EppoHttpException networkError(String message, Throwable cause) {
    return new EppoHttpException(message, cause, -1, false);
  }

  /**
   * Creates an exception for HTTP error responses.
   *
   * @param statusCode the HTTP status code
   * @param message the error message
   * @return a new HTTP error exception
   */
  public static EppoHttpException httpError(int statusCode, String message) {
    return new EppoHttpException(message, null, statusCode, false);
  }

  /**
   * Returns the HTTP status code associated with this error.
   *
   * @return the status code, or -1 for network errors
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Checks if this error is due to invalid authentication.
   *
   * @return true if this is an authentication error
   */
  public boolean isUnauthorized() {
    return isUnauthorized;
  }
}

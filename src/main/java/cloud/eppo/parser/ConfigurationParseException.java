package cloud.eppo.parser;

/**
 * Exception thrown when configuration parsing fails.
 *
 * <p>This exception is thrown by {@link ConfigurationParser} implementations when JSON
 * deserialization or serialization fails.
 */
public class ConfigurationParseException extends RuntimeException {

  /**
   * Creates a new parse exception with a message.
   *
   * @param message the error message
   */
  public ConfigurationParseException(String message) {
    super(message);
  }

  /**
   * Creates a new parse exception with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public ConfigurationParseException(String message, Throwable cause) {
    super(message, cause);
  }
}

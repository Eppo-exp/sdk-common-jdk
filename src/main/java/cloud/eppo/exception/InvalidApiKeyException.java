package cloud.eppo.exception;

public class InvalidApiKeyException extends RuntimeException {
  public InvalidApiKeyException(String message) {
    super(message);
  }
}

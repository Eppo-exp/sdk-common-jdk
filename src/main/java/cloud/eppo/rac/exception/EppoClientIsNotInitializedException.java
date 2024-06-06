package cloud.eppo.rac.exception;

public class EppoClientIsNotInitializedException extends RuntimeException {
  public EppoClientIsNotInitializedException(String message) {
    super(message);
  }
}

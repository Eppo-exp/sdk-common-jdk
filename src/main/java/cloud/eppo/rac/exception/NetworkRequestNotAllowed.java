package cloud.eppo.rac.exception;

public class NetworkRequestNotAllowed extends RuntimeException {
  public NetworkRequestNotAllowed(String message) {
    super(message);
  }
}

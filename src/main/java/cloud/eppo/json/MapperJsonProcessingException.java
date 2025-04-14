package cloud.eppo.json;

public class MapperJsonProcessingException extends MapperException {
  public MapperJsonProcessingException(String message) {
    super(message);
  }

  public MapperJsonProcessingException(Throwable cause) {
    super(cause);
  }

  public MapperJsonProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}

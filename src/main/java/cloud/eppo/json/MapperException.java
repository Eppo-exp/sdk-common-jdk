package cloud.eppo.json;

import java.io.IOException;

public class MapperException extends IOException {
  public MapperException(String message) {
    super(message);
  }
  public MapperException(Throwable cause) {
    super(cause);
  }
  public MapperException(String message, Throwable cause) {
    super(message, cause);
  }
}

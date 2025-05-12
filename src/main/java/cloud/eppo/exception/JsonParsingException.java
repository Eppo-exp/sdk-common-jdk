package cloud.eppo.exception;

import java.io.IOException;

public class JsonParsingException extends Throwable {
  public JsonParsingException(IOException e) {
    super(e);
  }
}

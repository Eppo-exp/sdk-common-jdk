package cloud.eppo.helpers;

import cloud.eppo.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonJsonValidator implements Utils.JsonValidator {

  @Override
  public boolean isValidJson(String json) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(json) != null;
    } catch (JsonProcessingException e) {
      return false;
    }
  }
}

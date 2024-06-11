package cloud.eppo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static cloud.eppo.Utils.parseUtcISODateElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UtilsTest {
  @Test
  public void testParseUtcISODateElement() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree("\"2024-05-01T16:13:26.651Z\"");
    Date parsedDate = parseUtcISODateElement(jsonNode);
    Date expectedDate = new Date(1714580006651L);
    assertEquals(expectedDate, parsedDate);
    jsonNode = mapper.readTree("null");
    parsedDate = parseUtcISODateElement(jsonNode);
    assertNull(parsedDate);
    assertNull(parseUtcISODateElement(null));
  }
}

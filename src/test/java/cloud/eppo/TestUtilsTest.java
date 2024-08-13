package cloud.eppo;

import static cloud.eppo.Utils.parseUtcISODateNode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class TestUtilsTest {
  @Test
  public void testParseUtcISODateNode() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree("\"2024-05-01T16:13:26.651Z\"");
    Date parsedDate = parseUtcISODateNode(jsonNode);
    Date expectedDate = new Date(1714580006651L);
    assertEquals(expectedDate, parsedDate);
    jsonNode = mapper.readTree("null");
    parsedDate = parseUtcISODateNode(jsonNode);
    assertNull(parsedDate);
    assertNull(parseUtcISODateNode(null));
  }
}

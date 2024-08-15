package cloud.eppo;

import static cloud.eppo.Utils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class UtilsTest {
  @Test
  public void testGetMd5Hash() {
    // empty string
    assertEquals("d41d8cd98f00b204e9800998ecf8427e", getMD5Hex(""));
    // leading zero
    assertEquals("0212de0d90804f17d2b7bab512cd2f0f", getMD5Hex("input-59"));
    // zero first byte
    assertEquals("00dd33988da4202fc1990a4dfa7ee18b", getMD5Hex("input-411"));
    // zero middle byte
    assertEquals("448614887a99f16179b400cfccceb72d", getMD5Hex("input-62"));
    // zero last byte
    assertEquals("429fb7196ccb2978443a0de8da180e00", getMD5Hex("input-34"));
  }

  @Test
  public void testGetShard() {
    // Shard is the first 8 digits read as a number and modulo into the space
    int computedShard = (int) (Long.parseLong(getMD5Hex("shard me").substring(0, 8), 16) % 10000);
    int shardFromGetShard = getShard("shard me", 10000);
    assertEquals(computedShard, shardFromGetShard);

    // Total shards is respected
    assertEquals(538, getShard("shard me", 10000));
    assertEquals(538, getShard("shard me", 1000));
    assertEquals(38, getShard("shard me", 100));
    assertEquals(8, getShard("shard me", 10));
  }

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

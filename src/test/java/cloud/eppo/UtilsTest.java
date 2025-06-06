package cloud.eppo;

import static cloud.eppo.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
  public void testGetMd5HashThreadSafe() {
    final AtomicBoolean interferenceEncountered = new AtomicBoolean(false);
    int numThreads = 2;
    try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
      for (int i = 0; i < numThreads; i += 1) {
        pool.execute(() -> {
          if (testForMd5Interference()) {
            interferenceEncountered.set(true);
          }
        });
      }
      pool.shutdown();
      assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
      assertFalse(interferenceEncountered.get());
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  private boolean testForMd5Interference() {
    boolean interferenceEncountered = false;
    for (int i = 0; i < 100; i += 1) {
      if (!getMD5Hex("input-62").equals("448614887a99f16179b400cfccceb72d")) {
        interferenceEncountered = true;
        break;
      }
    }
    return interferenceEncountered;
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

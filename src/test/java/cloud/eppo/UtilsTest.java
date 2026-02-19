package cloud.eppo;

import static cloud.eppo.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class UtilsTest {

  @AfterEach
  void resetCodec() {
    Utils.resetBase64Codec();
  }

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
    ExecutorService pool = Executors.newFixedThreadPool(numThreads);
    try {
      for (int i = 0; i < numThreads; i += 1) {
        pool.execute(
            () -> {
              if (testForMd5Interference()) {
                interferenceEncountered.set(true);
              }
            });
      }
      pool.shutdown();
    } finally {
      try {
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
      } catch (InterruptedException ex) {
        fail();
      }
      assertFalse(interferenceEncountered.get());
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
  public void testDateFormattingThreadSafety() throws InterruptedException {
    final AtomicBoolean collisionDetected = new AtomicBoolean(false);
    final AtomicInteger unexpectedExceptions = new AtomicInteger(0);
    final AtomicInteger incorrectFormatResults = new AtomicInteger(0);

    int numThreads = 20; // Spawn 20 threads
    int iterationsPerThread = 100; // Each thread will format 100 dates
    ExecutorService pool = Executors.newFixedThreadPool(numThreads);

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(numThreads);

    // Expected date: 2024-05-01T16:13:26.651Z -> 1714580006651L
    final long expectedTimestamp = 1714580006651L;

    try {
      for (int i = 0; i < numThreads; i++) {
        pool.execute(
            () -> {
              try {
                // Wait for all threads to start simultaneously
                startLatch.await();

                for (int j = 0; j < iterationsPerThread; j++) {
                  try {
                    Date originalDate = new Date(expectedTimestamp);
                    String formattedDate = getISODate(originalDate);
                    if (!formattedDate.equals("2024-05-01T16:13:26.651Z")) {
                      incorrectFormatResults.incrementAndGet();
                      collisionDetected.set(true);
                    }
                  } catch (Exception e) {
                    unexpectedExceptions.incrementAndGet();
                  }
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                finishLatch.countDown();
              }
            });
      }

      // Start all threads simultaneously to maximize contention
      startLatch.countDown();

      // Wait for all threads to complete
      assertTrue(
          finishLatch.await(30, TimeUnit.SECONDS), "Test threads did not complete within timeout");

    } finally {
      pool.shutdown();
      assertTrue(
          pool.awaitTermination(5, TimeUnit.SECONDS),
          "Thread pool did not shutdown within timeout");
    }

    // Print diagnostic information
    System.out.println("Unexpected exceptions: " + unexpectedExceptions.get());
    System.out.println("Incorrect format results: " + incorrectFormatResults.get());
    System.out.println("Total operations: " + (numThreads * iterationsPerThread));

    String failureMessage =
        "SimpleDateFormat thread-safety issue detected! "
            + "Exceptions: "
            + unexpectedExceptions.get()
            + ", Incorrect results: "
            + incorrectFormatResults.get();
    assertFalse(collisionDetected.get(), failureMessage);
    assertEquals(0, unexpectedExceptions.get(), failureMessage);
  }

  @Test
  void testCustomBase64Codec() {
    AtomicBoolean encodeCalled = new AtomicBoolean(false);
    AtomicBoolean decodeCalled = new AtomicBoolean(false);

    Utils.Base64Codec customCodec =
        new Utils.Base64Codec() {
          @Override
          public String base64Encode(String input) {
            encodeCalled.set(true);
            return "encoded:" + input;
          }

          @Override
          public String base64Decode(String input) {
            decodeCalled.set(true);
            return "decoded:" + input;
          }
        };

    Utils.setBase64Codec(customCodec);

    assertEquals("encoded:test", Utils.base64Encode("test"));
    assertTrue(encodeCalled.get());

    assertEquals("decoded:test", Utils.base64Decode("test"));
    assertTrue(decodeCalled.get());
  }

  @Test
  void testBase64EncodeDecodeDefault() {
    // Test null handling
    assertNull(Utils.base64Encode(null));
    assertNull(Utils.base64Decode(null));

    // Test encoding
    String original = "Hello, World!";
    String encoded = Utils.base64Encode(original);
    assertEquals("SGVsbG8sIFdvcmxkIQ==", encoded);

    // Test decoding
    String decoded = Utils.base64Decode(encoded);
    assertEquals(original, decoded);

    // Test round-trip
    assertEquals(original, Utils.base64Decode(Utils.base64Encode(original)));
  }

  @Test
  void testSetBase64CodecWithNullThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> Utils.setBase64Codec(null));
  }
}

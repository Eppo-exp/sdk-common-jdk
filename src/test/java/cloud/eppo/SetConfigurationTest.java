package cloud.eppo;

import static cloud.eppo.helpers.TestUtils.mockConfigurationClientError;
import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.Configuration;
import cloud.eppo.logging.AssignmentLogger;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SetConfigurationTest {

  private MemoryOnlyConfigurationStore configStore;
  private BaseEppoClient<Configuration, JsonNode> client;

  @BeforeEach
  void setUp() {
    configStore = new MemoryOnlyConfigurationStore();
    client =
        new BaseEppoClient<>(
            "test-api-key",
            "test-sdk",
            "1.0.0",
            null,
            Mockito.mock(AssignmentLogger.class),
            null,
            configStore,
            true, // graceful mode
            false,
            false,
            null,
            null,
            null,
            new JacksonConfigurationParser(),
            mockConfigurationClientError());
  }

  @Test
  void testSetConfigurationUpdatesGetConfiguration() {
    assertTrue(client.getConfiguration().isEmpty());
    Configuration config = Configuration.emptyConfig();
    client.setConfiguration(config);
    assertSame(config, client.getConfiguration());
  }

  @Test
  void testSetConfigurationFiresSubscribers() throws Exception {
    BlockingQueue<Configuration> received = new LinkedBlockingQueue<>();
    client.onConfigurationChange(received::add);

    Configuration config = Configuration.emptyConfig();
    client.setConfiguration(config);

    assertSame(config, received.poll(3, TimeUnit.SECONDS));
  }

  @Test
  void testSetConfigurationFiresMultipleSubscribers() throws Exception {
    CountDownLatch notified = new CountDownLatch(2);
    AtomicInteger count1 = new AtomicInteger(0);
    AtomicInteger count2 = new AtomicInteger(0);

    client.onConfigurationChange(
        c -> {
          count1.incrementAndGet();
          notified.countDown();
        });
    client.onConfigurationChange(
        c -> {
          count2.incrementAndGet();
          notified.countDown();
        });

    client.setConfiguration(Configuration.emptyConfig());

    assertTrue(notified.await(3, TimeUnit.SECONDS));
    assertEquals(1, count1.get());
    assertEquals(1, count2.get());
  }

  @Test
  void testSetConfigurationNullGracefulModeIgnored() {
    // In graceful mode, null should be silently ignored
    assertDoesNotThrow(() -> client.setConfiguration(null));
    assertTrue(client.getConfiguration().isEmpty()); // store unchanged
  }

  @Test
  void testSetConfigurationNullNonGracefulModeThrows() {
    // Switch to non-graceful mode
    client.setIsGracefulFailureMode(false);
    assertThrows(IllegalArgumentException.class, () -> client.setConfiguration(null));
  }

  @Test
  void testSetConfigurationLastWriteWins() {
    Configuration first = Configuration.emptyConfig();
    Configuration second = Configuration.emptyConfig();

    client.setConfiguration(first);
    assertSame(first, client.getConfiguration());

    client.setConfiguration(second);
    assertSame(second, client.getConfiguration());
  }

  @Test
  void testSetConfigurationIsReadableBeforeReturn() throws Exception {
    Configuration config = Configuration.emptyConfig();
    CompletableFuture<Configuration> captured = new CompletableFuture<>();
    client.onConfigurationChange(c -> captured.complete(client.getConfiguration()));

    client.setConfiguration(config);

    assertSame(config, client.getConfiguration());
    assertSame(config, captured.get(3, TimeUnit.SECONDS));
  }

  @Test
  void testSetConfigurationDoesNotFireUnsubscribedSubscribers() throws Exception {
    AtomicInteger count = new AtomicInteger(0);
    CompletableFuture<Void> firstNotification = new CompletableFuture<>();
    Runnable unsubscribe =
        client.onConfigurationChange(
            c -> {
              count.incrementAndGet();
              firstNotification.complete(null);
            });

    client.setConfiguration(Configuration.emptyConfig());
    firstNotification.get(3, TimeUnit.SECONDS);
    assertEquals(1, count.get());

    unsubscribe.run();
    CompletableFuture<Void> secondNotification = new CompletableFuture<>();
    client.onConfigurationChange(c -> secondNotification.complete(null));
    client.setConfiguration(Configuration.emptyConfig());
    secondNotification.get(3, TimeUnit.SECONDS);
    assertEquals(1, count.get());
  }
}

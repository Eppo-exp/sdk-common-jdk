package cloud.eppo;

import static cloud.eppo.helpers.TestUtils.mockConfigurationClientError;
import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.Configuration;
import cloud.eppo.logging.AssignmentLogger;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SetConfigurationTest {

  private ConfigurationStore configStore;
  private BaseEppoClient<Configuration, JsonNode> client;

  @BeforeEach
  void setUp() {
    configStore = new ConfigurationStore();
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
  void testSetConfigurationFiresSubscribers() {
    List<Configuration> received = new ArrayList<>();
    client.onConfigurationChange(received::add);

    Configuration config = Configuration.emptyConfig();
    client.setConfiguration(config);

    assertEquals(1, received.size());
    assertSame(config, received.get(0));
  }

  @Test
  void testSetConfigurationFiresMultipleSubscribers() {
    AtomicInteger count1 = new AtomicInteger(0);
    AtomicInteger count2 = new AtomicInteger(0);

    client.onConfigurationChange(c -> count1.incrementAndGet());
    client.onConfigurationChange(c -> count2.incrementAndGet());

    client.setConfiguration(Configuration.emptyConfig());

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
    // Sequential writes verify second call overwrites first. Concurrent last-write-wins
    // is correct by Java memory model (volatile write in ConfigurationStore), so sequential
    // testing is sufficient to confirm the basic contract.
    Configuration first = Configuration.emptyConfig();
    Configuration second = Configuration.emptyConfig();

    client.setConfiguration(first);
    assertSame(first, client.getConfiguration());

    client.setConfiguration(second);
    assertSame(second, client.getConfiguration());
  }

  @Test
  void testSetConfigurationIsReadableBeforeReturn() {
    // Verify the store is updated synchronously (readable immediately)
    Configuration config = Configuration.emptyConfig();
    final Configuration[] captured = {null};
    client.onConfigurationChange(c -> captured[0] = client.getConfiguration());

    client.setConfiguration(config);

    assertSame(config, captured[0]);
  }

  @Test
  void testSetConfigurationDoesNotFireUnsubscribedSubscribers() {
    AtomicInteger count = new AtomicInteger(0);
    Runnable unsubscribe = client.onConfigurationChange(c -> count.incrementAndGet());

    client.setConfiguration(Configuration.emptyConfig());
    assertEquals(1, count.get());

    unsubscribe.run();
    client.setConfiguration(Configuration.emptyConfig());
    assertEquals(1, count.get()); // no second call
  }
}

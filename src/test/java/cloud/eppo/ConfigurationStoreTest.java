package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.Configuration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigurationStoreTest {

  private ConfigurationStore store;

  @BeforeEach
  void setUp() {
    store = new ConfigurationStore();
  }

  @Test
  void testSubscriberCalledOnSave() {
    List<Configuration> received = new ArrayList<>();
    store.subscribe(received::add);

    Configuration config = Configuration.emptyConfig();
    store.saveConfiguration(config).join();

    assertEquals(1, received.size());
    assertSame(config, received.get(0));
  }

  @Test
  void testMultipleSubscribersCalledOnSave() {
    AtomicInteger count1 = new AtomicInteger(0);
    AtomicInteger count2 = new AtomicInteger(0);

    store.subscribe(c -> count1.incrementAndGet());
    store.subscribe(c -> count2.incrementAndGet());

    store.saveConfiguration(Configuration.emptyConfig()).join();

    assertEquals(1, count1.get());
    assertEquals(1, count2.get());
  }

  @Test
  void testUnsubscribeViaRunnable() {
    AtomicInteger count = new AtomicInteger(0);
    Runnable unsubscribe = store.subscribe(c -> count.incrementAndGet());

    store.saveConfiguration(Configuration.emptyConfig()).join();
    assertEquals(1, count.get());

    unsubscribe.run();
    store.saveConfiguration(Configuration.emptyConfig()).join();
    assertEquals(1, count.get()); // no second notification
  }

  @Test
  void testUnsubscribeByReference() {
    AtomicInteger count = new AtomicInteger(0);
    Consumer<Configuration> callback = c -> count.incrementAndGet();
    store.subscribe(callback);

    store.saveConfiguration(Configuration.emptyConfig()).join();
    assertEquals(1, count.get());

    assertTrue(store.unsubscribe(callback));
    store.saveConfiguration(Configuration.emptyConfig()).join();
    assertEquals(1, count.get());
  }

  @Test
  void testUnsubscribeNonExistentReturnsFalse() {
    Consumer<Configuration> callback = c -> {};
    assertFalse(store.unsubscribe(callback));
  }

  @Test
  void testUnsubscribeOneOfMultiple() {
    AtomicInteger count1 = new AtomicInteger(0);
    AtomicInteger count2 = new AtomicInteger(0);
    AtomicInteger count3 = new AtomicInteger(0);

    Consumer<Configuration> cb1 = c -> count1.incrementAndGet();
    Consumer<Configuration> cb2 = c -> count2.incrementAndGet();
    Consumer<Configuration> cb3 = c -> count3.incrementAndGet();

    store.subscribe(cb1);
    store.subscribe(cb2);
    store.subscribe(cb3);

    store.saveConfiguration(Configuration.emptyConfig()).join();
    assertEquals(1, count1.get());
    assertEquals(1, count2.get());
    assertEquals(1, count3.get());

    assertTrue(store.unsubscribe(cb2));

    store.saveConfiguration(Configuration.emptyConfig()).join();
    assertEquals(2, count1.get());
    assertEquals(1, count2.get()); // not called again
    assertEquals(2, count3.get());
  }

  @Test
  void testSaveConfigurationUpdatesStoredValue() {
    assertTrue(store.getConfiguration().isEmpty());

    Configuration config = Configuration.emptyConfig();
    store.saveConfiguration(config).join();

    assertSame(config, store.getConfiguration());
  }
}

package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.Configuration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigurationStoreTest {

  private MemoryOnlyConfigurationStore store;

  @BeforeEach
  void setUp() {
    store = new MemoryOnlyConfigurationStore();
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

  // Helper for tests that need to control persist behavior
  private static class ControllableStore extends AbstractConfigurationStore<Configuration> {
    private volatile Configuration config = Configuration.emptyConfig();
    private CompletableFuture<Void> persistFuture = CompletableFuture.completedFuture(null);

    @NotNull @Override
    public Configuration getConfiguration() {
      return config;
    }

    @Override
    protected CompletableFuture<Void> persist(@NotNull Configuration configuration) {
      this.config = configuration;
      return persistFuture;
    }

    void setPersistFuture(CompletableFuture<Void> future) {
      this.persistFuture = future;
    }
  }

  @Test
  void testSaveConfigurationDoesNotNotifyOnFailedPersist() {
    ControllableStore store = new ControllableStore();
    List<Configuration> received = new ArrayList<>();
    store.subscribe(received::add);

    CompletableFuture<Void> failedFuture = new CompletableFuture<>();
    store.setPersistFuture(failedFuture);
    failedFuture.completeExceptionally(new RuntimeException("persist failed"));

    Configuration config = Configuration.emptyConfig();
    store.saveConfiguration(config);

    assertTrue(received.isEmpty(), "Subscribers must not be notified when persist fails");
  }

  @Test
  void testSaveConfigurationDefersNotificationUntilPersistCompletes() {
    ControllableStore store = new ControllableStore();
    List<Configuration> received = new ArrayList<>();
    store.subscribe(received::add);

    CompletableFuture<Void> asyncPersist = new CompletableFuture<>();
    store.setPersistFuture(asyncPersist);

    Configuration config = Configuration.emptyConfig();
    store.saveConfiguration(config);

    assertTrue(received.isEmpty(), "Subscribers must not be notified before persist completes");

    asyncPersist.complete(null);

    assertEquals(1, received.size(), "Subscribers must be notified after persist completes");
  }

  @Test
  void testReentrantSaveFromSubscriberOnAsyncCompletingThreadIsIgnored()
      throws InterruptedException {
    ControllableStore store = new ControllableStore();
    java.util.concurrent.atomic.AtomicInteger notificationCount =
        new java.util.concurrent.atomic.AtomicInteger(0);
    java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);

    store.subscribe(
        config -> {
          int count = notificationCount.incrementAndGet();
          if (count == 1) {
            // Re-entrant save from the completing thread — must not recurse
            store.saveConfiguration(Configuration.emptyConfig());
            done.countDown();
          }
        });

    // Use an async persist so thenRun fires on a different thread
    CompletableFuture<Void> asyncPersist = new CompletableFuture<>();
    store.setPersistFuture(asyncPersist);

    store.saveConfiguration(Configuration.emptyConfig());

    // Complete persist on a separate thread
    new Thread(() -> asyncPersist.complete(null)).start();

    assertTrue(
        done.await(2, java.util.concurrent.TimeUnit.SECONDS), "subscriber should have been called");
    assertEquals(
        1, notificationCount.get(), "re-entrant save must not trigger a second notification");
  }
}

package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.Configuration;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigurationObserverDispatcherTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void disposedHandleDoesNotRetainItsCallback(boolean removeByReference) throws Exception {
    ConfigurationObserverDispatcher<Configuration> dispatcher =
        new ConfigurationObserverDispatcher<>(
            new MemoryOnlyConfigurationStore(), new QueuedExecutor());
    RetentionProbe removed = retainHandle(dispatcher, true, removeByReference);
    RetentionProbe active = retainHandle(dispatcher, false, removeByReference);
    WeakReference<Object> collectionControl = new WeakReference<>(new Object());
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
    while ((removed.callback.get() != null || collectionControl.get() != null)
        && System.nanoTime() < deadline) {
      System.gc();
      Thread.sleep(10);
    }
    assertNull(collectionControl.get(), "The collector must run for the reclamation assertion");
    assertNull(removed.callback.get(), "A retained disposed handle must release its callback");
    assertNotNull(active.callback.get(), "A live registration must retain its callback");
    // Keep both handles live across collection, and retain idempotent removal.
    removed.handle.run();
    active.handle.run();
  }

  private static RetentionProbe retainHandle(
      ConfigurationObserverDispatcher<Configuration> dispatcher,
      boolean remove,
      boolean removeByReference) {
    Consumer<Configuration> callback =
        new Consumer<Configuration>() {
          @Override
          public void accept(Configuration configuration) {}
        };
    Runnable handle = dispatcher.subscribe(callback);
    WeakReference<Consumer<Configuration>> reference = new WeakReference<>(callback);
    if (remove) {
      if (removeByReference) {
        assertTrue(dispatcher.unsubscribe(callback));
      } else {
        handle.run();
      }
    }
    return new RetentionProbe(handle, reference);
  }

  private static final class RetentionProbe {
    final Runnable handle;
    final WeakReference<Consumer<Configuration>> callback;

    RetentionProbe(Runnable handle, WeakReference<Consumer<Configuration>> callback) {
      this.handle = handle;
      this.callback = callback;
    }
  }

  @Test
  void queuesCapturedConfigurationsInOrderWithoutWaitingForObservers() {
    MemoryOnlyConfigurationStore store = new MemoryOnlyConfigurationStore();
    QueuedExecutor executor = new QueuedExecutor();
    ConfigurationObserverDispatcher<Configuration> dispatcher =
        new ConfigurationObserverDispatcher<>(store, executor);
    List<Configuration> received = new ArrayList<>();
    dispatcher.subscribe(received::add);
    Configuration first = Configuration.emptyConfig();
    Configuration second = Configuration.emptyConfig();

    assertDoesNotThrow(() -> store.saveConfiguration(first).join());
    assertDoesNotThrow(() -> store.saveConfiguration(second).join());
    assertSame(second, store.getConfiguration());
    assertTrue(received.isEmpty());

    executor.runAll();
    assertEquals(Arrays.asList(first, second), received);
  }

  @Test
  void returnedHandleRemovesOnlyItsRegistrationOfARepeatedConsumer() {
    MemoryOnlyConfigurationStore store = new MemoryOnlyConfigurationStore();
    QueuedExecutor executor = new QueuedExecutor();
    ConfigurationObserverDispatcher<Configuration> dispatcher =
        new ConfigurationObserverDispatcher<>(store, executor);
    AtomicInteger calls = new AtomicInteger();
    Consumer<Configuration> callback = ignored -> calls.incrementAndGet();
    Runnable firstHandle = dispatcher.subscribe(callback);
    dispatcher.subscribe(callback);
    store.saveConfiguration(Configuration.emptyConfig()).join();

    firstHandle.run();
    firstHandle.run();
    executor.runAll();
    assertEquals(1, calls.get());

    store.saveConfiguration(Configuration.emptyConfig()).join();
    executor.runAll();
    assertEquals(2, calls.get());
  }

  @Test
  void unsubscribeByReferenceSuppressesAllQueuedRegistrationsOfThatConsumer() {
    MemoryOnlyConfigurationStore store = new MemoryOnlyConfigurationStore();
    QueuedExecutor executor = new QueuedExecutor();
    ConfigurationObserverDispatcher<Configuration> dispatcher =
        new ConfigurationObserverDispatcher<>(store, executor);
    AtomicInteger calls = new AtomicInteger();
    Consumer<Configuration> callback = ignored -> calls.incrementAndGet();
    Runnable handle = dispatcher.subscribe(callback);
    dispatcher.subscribe(callback);
    store.saveConfiguration(Configuration.emptyConfig()).join();

    assertTrue(dispatcher.unsubscribe(callback));
    assertFalse(dispatcher.unsubscribe(callback));
    handle.run();
    dispatcher.subscribe(callback);
    executor.runAll();
    assertEquals(0, calls.get());

    store.saveConfiguration(Configuration.emptyConfig()).join();
    executor.runAll();
    assertEquals(1, calls.get());
  }

  @Test
  void unsubscribeUsesIdentityEvenWhenConsumersCompareEqual() {
    MemoryOnlyConfigurationStore store = new MemoryOnlyConfigurationStore();
    QueuedExecutor executor = new QueuedExecutor();
    ConfigurationObserverDispatcher<Configuration> dispatcher =
        new ConfigurationObserverDispatcher<>(store, executor);
    EqualConsumer first = new EqualConsumer();
    EqualConsumer second = new EqualConsumer();
    dispatcher.subscribe(first);
    dispatcher.subscribe(second);
    store.saveConfiguration(Configuration.emptyConfig()).join();

    assertTrue(dispatcher.unsubscribe(first));
    executor.runAll();
    assertEquals(0, first.calls.get());
    assertEquals(1, second.calls.get());
  }

  @Test
  void ordinaryObserverExceptionDoesNotEscapeOrStopOtherNotifications() {
    MemoryOnlyConfigurationStore store = new MemoryOnlyConfigurationStore();
    QueuedExecutor executor = new QueuedExecutor();
    ConfigurationObserverDispatcher<Configuration> dispatcher =
        new ConfigurationObserverDispatcher<>(store, executor);
    AtomicInteger healthyCalls = new AtomicInteger();
    dispatcher.subscribe(
        ignored -> {
          throw new IllegalStateException("customer failure");
        });
    dispatcher.subscribe(ignored -> healthyCalls.incrementAndGet());
    Configuration configuration = Configuration.emptyConfig();

    assertDoesNotThrow(() -> store.saveConfiguration(configuration).join());
    assertDoesNotThrow(executor::runAll);
    assertSame(configuration, store.getConfiguration());
    assertEquals(1, healthyCalls.get());
  }

  @Test
  void fullBacklogRejectsNewDeliveryWithoutBlockingOrRunningInline() throws Exception {
    MemoryOnlyConfigurationStore store = new MemoryOnlyConfigurationStore();
    ThreadPoolExecutor executor = ConfigurationObserverDispatcher.createExecutor(1, 30_000);
    ConfigurationObserverDispatcher<Configuration> dispatcher =
        new ConfigurationObserverDispatcher<>(store, executor);
    Configuration first = Configuration.emptyConfig();
    Configuration second = Configuration.emptyConfig();
    Configuration rejected = Configuration.emptyConfig();
    CountDownLatch enteredFirst = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);
    CountDownLatch deliveredSecond = new CountDownLatch(1);
    List<Configuration> received = Collections.synchronizedList(new ArrayList<>());
    List<Thread> callbackThreads = Collections.synchronizedList(new ArrayList<>());
    Thread updatingThread = Thread.currentThread();
    dispatcher.subscribe(
        configuration -> {
          received.add(configuration);
          callbackThreads.add(Thread.currentThread());
          if (configuration == first) {
            enteredFirst.countDown();
            await(releaseFirst);
          } else if (configuration == second) {
            deliveredSecond.countDown();
          }
        });
    try {
      store.saveConfiguration(first).join();
      assertTrue(enteredFirst.await(5, TimeUnit.SECONDS));
      store.saveConfiguration(second).join();
      assertEquals(1, executor.getQueue().size());

      assertDoesNotThrow(() -> store.saveConfiguration(rejected).join());
      assertSame(rejected, store.getConfiguration());
      assertEquals(Collections.singletonList(first), received);
      assertEquals(1, executor.getQueue().size());

      releaseFirst.countDown();
      assertTrue(deliveredSecond.await(5, TimeUnit.SECONDS));
      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
      assertEquals(Arrays.asList(first, second), received);
      assertTrue(callbackThreads.stream().noneMatch(thread -> thread == updatingThread));
    } finally {
      releaseFirst.countDown();
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void callbackDoesNotHoldRegistryOrStoreNotificationLocks() throws Exception {
    MemoryOnlyConfigurationStore store = new MemoryOnlyConfigurationStore();
    ThreadPoolExecutor executor = ConfigurationObserverDispatcher.createExecutor(4, 30_000);
    ExecutorService otherThread = Executors.newSingleThreadExecutor();
    ConfigurationObserverDispatcher<Configuration> dispatcher =
        new ConfigurationObserverDispatcher<>(store, executor);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicReference<Runnable> handle = new AtomicReference<>();
    Configuration first = Configuration.emptyConfig();
    Configuration replacement = Configuration.emptyConfig();
    handle.set(
        dispatcher.subscribe(
            ignored -> {
              entered.countDown();
              await(release);
            }));
    try {
      store.saveConfiguration(first).join();
      assertTrue(entered.await(5, TimeUnit.SECONDS));
      otherThread
          .submit(
              () -> {
                handle.get().run();
                Runnable newHandle = dispatcher.subscribe(ignored -> {});
                store.saveConfiguration(replacement).join();
                newHandle.run();
              })
          .get(5, TimeUnit.SECONDS);
      assertSame(replacement, store.getConfiguration());
    } finally {
      release.countDown();
      executor.shutdownNow();
      otherThread.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
      otherThread.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void lazyDaemonWorkerRetiresWhenIdleAndRestartsForLaterNotifications() throws Exception {
    MemoryOnlyConfigurationStore store = new MemoryOnlyConfigurationStore();
    ThreadPoolExecutor executor = ConfigurationObserverDispatcher.createExecutor(1, 20);
    ConfigurationObserverDispatcher<Configuration> dispatcher =
        new ConfigurationObserverDispatcher<>(store, executor);
    CompletableFuture<Thread> firstThread = new CompletableFuture<>();
    CompletableFuture<Thread> secondThread = new CompletableFuture<>();
    AtomicInteger calls = new AtomicInteger();
    dispatcher.subscribe(
        ignored -> {
          if (calls.incrementAndGet() == 1) {
            firstThread.complete(Thread.currentThread());
          } else {
            secondThread.complete(Thread.currentThread());
          }
        });
    try {
      assertEquals(0, executor.getPoolSize());
      store.saveConfiguration(Configuration.emptyConfig()).join();
      Thread first = firstThread.get(5, TimeUnit.SECONDS);
      assertTrue(first.isDaemon());
      assertTrue(first.getName().startsWith("eppo-configuration-observer-"));
      first.join(5_000);
      assertFalse(first.isAlive());
      assertEquals(0, executor.getPoolSize());

      store.saveConfiguration(Configuration.emptyConfig()).join();
      assertNotSame(first, secondThread.get(5, TimeUnit.SECONDS));
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void observerCanUnsubscribeDuringAStoreSynchronousSubscriptionReplay() throws Exception {
    CountDownLatch observerRemoved = new CountDownLatch(1);
    AtomicInteger storeRemovals = new AtomicInteger();
    MemoryOnlyConfigurationStore store =
        new MemoryOnlyConfigurationStore() {
          @Override
          public Runnable subscribe(Consumer<Configuration> callback) {
            callback.accept(Configuration.emptyConfig());
            await(observerRemoved);
            return storeRemovals::incrementAndGet;
          }
        };
    ThreadPoolExecutor executor = ConfigurationObserverDispatcher.createExecutor(1, 30_000);
    ConfigurationObserverDispatcher<Configuration> dispatcher =
        new ConfigurationObserverDispatcher<>(store, executor);
    AtomicReference<Consumer<Configuration>> callback = new AtomicReference<>();
    callback.set(
        ignored -> {
          dispatcher.unsubscribe(callback.get());
          observerRemoved.countDown();
        });
    try {
      Runnable handle = dispatcher.subscribe(callback.get());
      assertEquals(1, storeRemovals.get());
      handle.run();
      assertEquals(1, storeRemovals.get());
    } finally {
      observerRemoved.countDown();
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for test coordination");
      }
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for test coordination", failure);
    }
  }

  private static final class QueuedExecutor implements Executor {
    private final Queue<Runnable> tasks = new ArrayDeque<>();

    @Override
    public void execute(Runnable task) {
      tasks.add(task);
    }

    private void runAll() {
      Runnable task;
      while ((task = tasks.poll()) != null) {
        task.run();
      }
    }
  }

  private static final class EqualConsumer implements Consumer<Configuration> {
    private final AtomicInteger calls = new AtomicInteger();

    @Override
    public void accept(Configuration ignored) {
      calls.incrementAndGet();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof EqualConsumer;
    }

    @Override
    public int hashCode() {
      return 1;
    }
  }
}

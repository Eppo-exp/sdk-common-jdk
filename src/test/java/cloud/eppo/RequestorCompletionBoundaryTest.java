package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cloud.eppo.api.Configuration;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.http.EppoConfigurationClient;
import cloud.eppo.http.EppoConfigurationRequestFactory;
import cloud.eppo.http.EppoConfigurationResponse;
import cloud.eppo.parser.ConfigurationParser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RequestorCompletionBoundaryTest {
  private static final long TIMEOUT_SECONDS = 3;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void remoteContinuationCanFetchAgainWhileInitialSaveDrains(boolean joinAsync) throws Exception {
    Configuration cached = mock(Configuration.class);
    Configuration first = mock(Configuration.class);
    Configuration second = mock(Configuration.class);
    try (ControlledStore store = new ControlledStore(cached, false)) {
      EppoConfigurationRequestor<Configuration, Object> requestor = requestor(store, first, second);
      CompletableFuture<Boolean> initial =
          requestor.setInitialConfiguration(CompletableFuture.completedFuture(cached));
      CompletableFuture<Void> fetched = requestor.fetchAndSaveFromRemoteAsync();
      CompletableFuture<Void> continued = fetched.thenRun(() -> fetchAgain(requestor, joinAsync));
      Thread completer = daemon("complete-initial", () -> store.release.complete(null));

      await(continued);
      completer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
      assertFalse(completer.isAlive(), "A customer continuation must not hold the initial drain");
      assertTrue(await(initial));
      assertSame(second, store.getConfiguration());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void remoteContinuationDoesNotOccupySinglePersistenceThread(boolean joinAsync) throws Exception {
    Configuration first = mock(Configuration.class);
    Configuration second = mock(Configuration.class);
    try (ControlledStore store = new ControlledStore(first, true)) {
      EppoConfigurationRequestor<Configuration, Object> requestor = requestor(store, first, second);
      CompletableFuture<Void> fetched = requestor.fetchAndSaveFromRemoteAsync();
      CompletableFuture<Void> continued = fetched.thenRun(() -> fetchAgain(requestor, joinAsync));
      store.release.complete(null);

      await(continued);
      assertSame(second, store.getConfiguration());
    }
  }

  @Test
  void initialContinuationDoesNotOccupySinglePersistenceThread() throws Exception {
    Configuration cached = mock(Configuration.class);
    Configuration remote = mock(Configuration.class);
    try (ControlledStore store = new ControlledStore(cached, true)) {
      EppoConfigurationRequestor<Configuration, Object> requestor = requestor(store, remote);
      CompletableFuture<Boolean> initial =
          requestor.setInitialConfiguration(CompletableFuture.completedFuture(cached));
      CompletableFuture<Void> continued = initial.thenRun(requestor::fetchAndSaveFromRemote);
      store.release.complete(null);

      await(continued);
      assertSame(remote, store.getConfiguration());
    }
  }

  @Test
  void blockedCustomerContinuationDoesNotPreventLaterSynchronousPersistence() throws Exception {
    Configuration first = mock(Configuration.class);
    Configuration second = mock(Configuration.class);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch releaseCustomer = new CountDownLatch(1);
    try (ControlledStore store = new ControlledStore(first, true)) {
      EppoConfigurationRequestor<Configuration, Object> requestor = requestor(store, first, second);
      CompletableFuture<Void> fetched = requestor.fetchAndSaveFromRemoteAsync();
      CompletableFuture<Void> continued =
          fetched.thenRun(
              () -> {
                entered.countDown();
                awaitLatch(releaseCustomer);
              });
      store.release.complete(null);
      assertTrue(entered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      CompletableFuture<Void> later = new CompletableFuture<>();
      daemon(
          "later-sync-fetch",
          () -> {
            try {
              requestor.fetchAndSaveFromRemote();
              later.complete(null);
            } catch (Throwable failure) {
              later.completeExceptionally(failure);
            }
          });
      try {
        await(later);
        assertSame(second, store.getConfiguration());
        assertFalse(continued.isDone(), "The customer callback is deliberately still held");
      } finally {
        releaseCustomer.countDown();
      }
      await(continued);
    } finally {
      releaseCustomer.countDown();
    }
  }

  @Test
  void saturationRejectsBeforeHttpAndKeepsAcceptedResponseEligible() throws Exception {
    Configuration remote = mock(Configuration.class);
    ConfigurationResultPublisher publisher =
        new ConfigurationResultPublisher(4, 10, TimeUnit.MILLISECONDS);
    AtomicInteger requests = new AtomicInteger();
    AtomicBoolean immediateResponse = new AtomicBoolean();
    AtomicReference<CompletableFuture<EppoConfigurationResponse>> response =
        new AtomicReference<>();
    EppoConfigurationClient client =
        request -> {
          requests.incrementAndGet();
          if (immediateResponse.get()) {
            return CompletableFuture.completedFuture(success());
          }
          CompletableFuture<EppoConfigurationResponse> pending = new CompletableFuture<>();
          response.set(pending);
          return pending;
        };
    CountDownLatch releaseCustomer = new CountDownLatch(1);
    List<CompletableFuture<Void>> continued = new ArrayList<>();
    try (ControlledStore store = new ControlledStore(null, false)) {
      EppoConfigurationRequestor<Configuration, Object> requestor =
          requestor(store, publisher, client, remote);
      for (int index = 0; index < 3; index++) {
        CountDownLatch entered = new CountDownLatch(1);
        CompletableFuture<Void> fetched = requestor.fetchAndSaveFromRemoteAsync();
        continued.add(
            fetched.thenRun(
                () -> {
                  entered.countDown();
                  awaitLatch(releaseCustomer);
                }));
        response.get().complete(success());
        assertTrue(entered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      }
      CompletableFuture<Void> accepted = requestor.fetchAndSaveFromRemoteAsync();
      CompletableFuture<EppoConfigurationResponse> acceptedResponse = response.get();
      CompletableFuture<Void> rejected = requestor.fetchAndSaveFromRemoteAsync();
      ExecutionException failure = assertThrows(ExecutionException.class, () -> await(rejected));
      assertInstanceOf(RejectedExecutionException.class, failure.getCause());
      assertEquals(
          4, requests.get(), "A rejected load must not start HTTP or cancel an accepted load");
      assertFalse(accepted.isDone());
      assertFalse(
          await(requestor.setInitialConfiguration(CompletableFuture.completedFuture(remote))));
      // Synchronous loads do not reserve a public result publisher, even when all four slots are
      // held.
      immediateResponse.set(true);
      requestor.fetchAndSaveFromRemote();
      assertSame(remote, store.getConfiguration());
      acceptedResponse.complete(success());
      await(accepted);
      releaseCustomer.countDown();
      for (CompletableFuture<Void> callback : continued) {
        await(callback);
      }
      awaitCondition(() -> publisher.workerCount() == 0);
      assertFalse(
          await(requestor.setInitialConfiguration(CompletableFuture.completedFuture(remote))),
          "Rejected initial registration can be retried after result capacity becomes available");
      assertThrows(
          IllegalStateException.class,
          () -> requestor.setInitialConfiguration(CompletableFuture.completedFuture(remote)));
    } finally {
      releaseCustomer.countDown();
    }
  }

  @Test
  void nestedAsyncJoinsFailAdmissionAtCapacityAndReleaseEverySlot() throws Exception {
    Configuration remote = mock(Configuration.class);
    ConfigurationResultPublisher publisher =
        new ConfigurationResultPublisher(4, 10, TimeUnit.MILLISECONDS);
    AtomicInteger requests = new AtomicInteger();
    AtomicReference<CompletableFuture<EppoConfigurationResponse>> response =
        new AtomicReference<>();
    EppoConfigurationClient client =
        request -> {
          requests.incrementAndGet();
          CompletableFuture<EppoConfigurationResponse> pending = new CompletableFuture<>();
          response.set(pending);
          return pending;
        };
    try (ControlledStore store = new ControlledStore(null, false)) {
      EppoConfigurationRequestor<Configuration, Object> requestor =
          requestor(store, publisher, client, remote);
      CompletableFuture<Void> nested = nestedFetch(requestor, response, 8);
      ExecutionException failure = assertThrows(ExecutionException.class, () -> await(nested));
      assertInstanceOf(RejectedExecutionException.class, failure.getCause());
      assertEquals(4, requests.get());
      awaitCondition(() -> publisher.workerCount() == 0);
      CompletableFuture<Void> subsequent = requestor.fetchAndSaveFromRemoteAsync();
      response.get().complete(success());
      await(subsequent);
    }
  }

  @Test
  void cancellationAndReplacementDoNotLeakPublicationSlotsOrAdmitLateResponses() throws Exception {
    Configuration remote = mock(Configuration.class);
    ConfigurationResultPublisher publisher =
        new ConfigurationResultPublisher(2, 10, TimeUnit.MILLISECONDS);
    AtomicInteger requests = new AtomicInteger();
    List<CompletableFuture<EppoConfigurationResponse>> responses = new ArrayList<>();
    EppoConfigurationClient client =
        request -> {
          requests.incrementAndGet();
          CompletableFuture<EppoConfigurationResponse> pending = new CompletableFuture<>();
          responses.add(pending);
          return pending;
        };
    try (ControlledStore store = new ControlledStore(null, false)) {
      EppoConfigurationRequestor<Configuration, Object> requestor =
          requestor(store, publisher, client, remote);
      assertEquals(
          0, publisher.workerCount(), "Publication threads are created only for ready results");
      for (int iteration = 0; iteration < 12; iteration++) {
        CompletableFuture<Void> replaced = requestor.fetchAndSaveFromRemoteAsync();
        CompletableFuture<Void> cancelled = requestor.fetchAndSaveFromRemoteAsync();
        assertThrows(CancellationException.class, () -> await(replaced));
        assertTrue(cancelled.cancel(true));
        awaitCondition(() -> publisher.workerCount() == 0);
      }
      assertEquals(24, requests.get());
      for (CompletableFuture<EppoConfigurationResponse> pending : responses) {
        assertTrue(
            pending.complete(success()), "Future cancellation does not promise I/O interruption");
      }
      assertTrue(
          store.getConfiguration().isEmpty(), "Cancelled HTTP responses cannot enter persistence");
      CompletableFuture<Void> subsequent = requestor.fetchAndSaveFromRemoteAsync();
      responses.get(responses.size() - 1).complete(success());
      await(subsequent);
      assertSame(remote, store.getConfiguration());
      awaitCondition(() -> publisher.workerCount() == 0);
    }
  }

  @Test
  void synchronousSetupFailureReleasesPublicationSlot() throws Exception {
    ConfigurationResultPublisher publisher =
        new ConfigurationResultPublisher(1, 10, TimeUnit.MILLISECONDS);
    AtomicInteger requests = new AtomicInteger();
    EppoConfigurationClient client =
        request -> {
          requests.incrementAndGet();
          throw new IllegalStateException("HTTP setup failed");
        };
    try (ControlledStore store = new ControlledStore(null, false)) {
      EppoConfigurationRequestor<Configuration, Object> requestor =
          requestor(store, publisher, client, mock(Configuration.class));
      for (int iteration = 0; iteration < 4; iteration++) {
        ExecutionException failure =
            assertThrows(
                ExecutionException.class, () -> await(requestor.fetchAndSaveFromRemoteAsync()));
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        awaitCondition(() -> publisher.workerCount() == 0);
      }
      assertEquals(4, requests.get());
    }
  }

  @Test
  void fatalSetupFailureIsRethrownWithoutLeakingPublicationSlot() throws Exception {
    ConfigurationResultPublisher publisher =
        new ConfigurationResultPublisher(1, 10, TimeUnit.MILLISECONDS);
    AtomicInteger requests = new AtomicInteger();
    EppoConfigurationClient client =
        request -> {
          requests.incrementAndGet();
          throw new AssertionError("HTTP setup failed fatally");
        };
    try (ControlledStore store = new ControlledStore(null, false)) {
      EppoConfigurationRequestor<Configuration, Object> requestor =
          requestor(store, publisher, client, mock(Configuration.class));
      for (int iteration = 0; iteration < 4; iteration++) {
        assertThrows(AssertionError.class, requestor::fetchAndSaveFromRemoteAsync);
      }
      assertEquals(4, requests.get());
      assertEquals(0, publisher.workerCount());
    }
  }

  private static CompletableFuture<Void> nestedFetch(
      EppoConfigurationRequestor<Configuration, Object> requestor,
      AtomicReference<CompletableFuture<EppoConfigurationResponse>> response,
      int remaining) {
    if (remaining == 0) {
      return CompletableFuture.completedFuture(null);
    }
    CompletableFuture<Void> fetched = requestor.fetchAndSaveFromRemoteAsync();
    CompletableFuture<EppoConfigurationResponse> pending = response.getAndSet(null);
    if (pending == null) {
      return fetched;
    }
    CompletableFuture<Void> continued =
        fetched.thenRun(() -> nestedFetch(requestor, response, remaining - 1).join());
    pending.complete(success());
    return continued;
  }

  private static EppoConfigurationResponse success() {
    return EppoConfigurationResponse.success(200, "remote", new byte[] {1});
  }

  private static void awaitCondition(BooleanSupplier condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
    while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
      Thread.sleep(5);
    }
    assertTrue(condition.getAsBoolean(), "Condition was not met before the test deadline");
  }

  private static void fetchAgain(
      EppoConfigurationRequestor<Configuration, Object> requestor, boolean joinAsync) {
    if (joinAsync) {
      requestor.fetchAndSaveFromRemoteAsync().join();
    } else {
      requestor.fetchAndSaveFromRemote();
    }
  }

  private static Thread daemon(String name, Runnable task) {
    Thread thread = new Thread(task, "requestor-boundary-" + name);
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  private static void awaitLatch(CountDownLatch latch) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          latch.await();
          return;
        } catch (InterruptedException ignored) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static <T> T await(CompletableFuture<T> result) throws Exception {
    return result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @SuppressWarnings("unchecked")
  private static EppoConfigurationRequestor<Configuration, Object> requestor(
      ControlledStore store, Configuration... configurations) {
    return requestor(
        store,
        new ConfigurationResultPublisher(),
        request -> CompletableFuture.completedFuture(success()),
        configurations);
  }

  @SuppressWarnings("unchecked")
  private static EppoConfigurationRequestor<Configuration, Object> requestor(
      ControlledStore store,
      ConfigurationResultPublisher publisher,
      EppoConfigurationClient client,
      Configuration... configurations) {
    ConfigurationParser<Configuration, Object> parser = mock(ConfigurationParser.class);
    when(parser.parseFlagConfig(any())).thenReturn(mock(FlagConfigResponse.class));
    AtomicInteger position = new AtomicInteger();
    when(parser.buildConfig(any(), any(), any(), any()))
        .thenAnswer(
            ignored ->
                configurations[Math.min(position.getAndIncrement(), configurations.length - 1)]);
    return new EppoConfigurationRequestor<>(
        store,
        false,
        parser,
        client,
        new EppoConfigurationRequestFactory("https://example.invalid", "test", "test", "1"),
        publisher);
  }

  private static final class ControlledStore
      implements IConfigurationStore<Configuration>, AutoCloseable {
    private final Configuration held;
    private final ExecutorService executor;
    private final CompletableFuture<Void> release = new CompletableFuture<>();
    private volatile Configuration current = Configuration.emptyConfig();

    private ControlledStore(Configuration held, boolean singleThread) {
      this.held = held;
      executor =
          singleThread
              ? Executors.newSingleThreadExecutor(
                  task -> {
                    Thread thread = new Thread(task, "requestor-boundary-persistence");
                    thread.setDaemon(true);
                    return thread;
                  })
              : null;
    }

    @Override
    public Configuration getConfiguration() {
      return current;
    }

    @Override
    public CompletableFuture<Void> saveConfiguration(@NotNull Configuration configuration) {
      CompletableFuture<Void> admitted =
          configuration == held ? release : CompletableFuture.completedFuture(null);
      return admitted.thenCompose(
          ignored -> {
            Runnable install = () -> current = configuration;
            if (executor == null) {
              install.run();
              return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.runAsync(install, executor);
          });
    }

    @Override
    public Runnable subscribe(Consumer<Configuration> callback) {
      return () -> {};
    }

    @Override
    public boolean unsubscribe(Consumer<Configuration> callback) {
      return false;
    }

    @Override
    public void close() {
      if (executor != null) {
        executor.shutdownNow();
      }
    }
  }
}

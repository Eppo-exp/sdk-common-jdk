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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InitialRemotePrecedenceTest {
  private static final long TIMEOUT_SECONDS = 5;

  @Test
  void pendingRemoteSaveTakesPrecedenceOverLateCacheInMemoryAndOnDisk() throws Exception {
    exerciseQueuedSave(false);
  }

  @Test
  void firstFailedRemoteSaveStillAllowsCacheFallback() throws Exception {
    exerciseQueuedSave(true);
  }

  private void exerciseQueuedSave(boolean failRemote) throws Exception {
    TestStore store = new TestStore(true);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Void> remoteWrite = store.holdNextWrite();
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    CompletableFuture<Boolean> accepted = fixture.requestor.setInitialConfiguration(initial);

    CompletableFuture<Void> fetched = fixture.requestor.fetchAndSaveFromRemoteAsync();
    assertEquals(1, store.submitted.size());
    assertFalse(fetched.isDone());
    initial.complete(fixture.cached);

    if (failRemote) {
      remoteWrite.completeExceptionally(new IllegalStateException("remote write failed"));
      assertThrows(ExecutionException.class, () -> await(fetched));
      assertTrue(await(accepted));
      assertSame(fixture.cached, store.current);
      assertArrayEquals(new byte[] {2}, store.disk);
    } else {
      remoteWrite.complete(null);
      await(fetched);
      assertFalse(await(accepted));
      assertSame(fixture.remote, store.current);
      assertArrayEquals(new byte[] {1}, store.disk);
      assertEquals(1, store.submitted.size());
    }
  }

  @Test
  void earlierRemoteSuccessSurvivesLaterFailureAndLateCache() throws Exception {
    TestStore store = new TestStore(true);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    CompletableFuture<Boolean> accepted = fixture.requestor.setInitialConfiguration(initial);
    await(fixture.requestor.fetchAndSaveFromRemoteAsync());
    CompletableFuture<Void> secondWrite = store.holdNextWrite();
    CompletableFuture<Void> secondFetch = fixture.requestor.fetchAndSaveFromRemoteAsync();
    secondWrite.completeExceptionally(new IllegalStateException("later write failed"));
    assertThrows(ExecutionException.class, () -> await(secondFetch));

    initial.complete(fixture.cached);

    assertFalse(await(accepted));
    assertSame(fixture.remote, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
    assertEquals(2, store.submitted.size());
  }

  @Test
  void reentrantInitialCompletionSeesRemoteSaveAlreadyReserved() throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    CompletableFuture<Boolean> accepted = fixture.requestor.setInitialConfiguration(initial);
    store.beforeSaveReturns = () -> initial.complete(fixture.cached);

    await(fixture.requestor.fetchAndSaveFromRemoteAsync());

    assertFalse(await(accepted));
    assertSame(fixture.remote, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
    assertEquals(1, store.submitted.size());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void overlappingRemoteSuccessAndFailureCannotAdmitLateCache(boolean failureCompletesFirst)
      throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    CompletableFuture<Boolean> accepted = fixture.requestor.setInitialConfiguration(initial);
    CompletableFuture<Void> firstWrite = store.holdNextWrite();
    fixture.requestor.fetchAndSaveFromRemoteAsync();
    CompletableFuture<Void> secondWrite = store.holdNextWrite();
    CompletableFuture<Void> secondFetch = fixture.requestor.fetchAndSaveFromRemoteAsync();
    assertEquals(2, store.submitted.size());

    if (failureCompletesFirst) {
      secondWrite.completeExceptionally(new IllegalStateException("second write failed"));
      assertThrows(ExecutionException.class, () -> await(secondFetch));
      initial.complete(fixture.cached);
      assertFalse(accepted.isDone(), "The other remote save still owns persistence");
      firstWrite.complete(null);
    } else {
      firstWrite.complete(null);
      initial.complete(fixture.cached);
      secondWrite.completeExceptionally(new IllegalStateException("second write failed"));
      assertThrows(ExecutionException.class, () -> await(secondFetch));
    }

    assertFalse(await(accepted));
    assertSame(fixture.remote, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
    assertEquals(2, store.submitted.size());
  }

  @Test
  void initialSaveAdmittedFirstCannotFinishAfterAndReplaceRemote() throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Void> initialWrite = store.holdNextWrite();
    CompletableFuture<Boolean> accepted =
        fixture.requestor.setInitialConfiguration(
            CompletableFuture.completedFuture(fixture.cached));

    CompletableFuture<Void> fetched = fixture.requestor.fetchAndSaveFromRemoteAsync();
    initialWrite.complete(null);
    assertTrue(await(accepted));
    await(fetched);

    assertSame(fixture.remote, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
  }

  @Test
  void failedInitialSaveUnblocksAlreadyAdmittedRemoteSave() throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Void> initialWrite = store.holdNextWrite();
    CompletableFuture<Boolean> accepted =
        fixture.requestor.setInitialConfiguration(
            CompletableFuture.completedFuture(fixture.cached));

    CompletableFuture<Void> fetched = fixture.requestor.fetchAndSaveFromRemoteAsync();
    assertEquals(
        1, store.submitted.size(), "Remote persistence waits for the initial save to settle");
    assertFalse(fetched.isDone());
    initialWrite.completeExceptionally(new IllegalStateException("initial write failed"));

    assertFalse(await(accepted));
    await(fetched);
    assertSame(fixture.remote, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
    assertEquals(2, store.submitted.size());
  }

  @Test
  void synchronousInitialStoreFailureDoesNotBlockLaterRemoteSave() throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture = new Fixture(store);
    store.nextSynchronousFailure = new IllegalStateException("store rejected initial write");

    assertFalse(
        await(
            fixture.requestor.setInitialConfiguration(
                CompletableFuture.completedFuture(fixture.cached))));
    await(fixture.requestor.fetchAndSaveFromRemoteAsync());

    assertSame(fixture.remote, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
    assertEquals(2, store.submitted.size());
  }

  @Test
  void cancellingCallerFetchDoesNotReleaseRemotePersistenceOwnership() throws Exception {
    TestStore store = new TestStore(true);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    CompletableFuture<Boolean> accepted = fixture.requestor.setInitialConfiguration(initial);
    CompletableFuture<Void> remoteWrite = store.holdNextWrite();
    CompletableFuture<Void> fetched = fixture.requestor.fetchAndSaveFromRemoteAsync();
    assertTrue(fetched.cancel(true));

    initial.complete(fixture.cached);
    remoteWrite.complete(null);

    assertFalse(await(accepted));
    assertSame(fixture.remote, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
    assertEquals(1, store.submitted.size());
  }

  @Test
  void everyOverlappingRemoteMustFailBeforeCacheCanBeAdmitted() throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    CompletableFuture<Boolean> accepted = fixture.requestor.setInitialConfiguration(initial);
    CompletableFuture<Void> firstWrite = store.holdNextWrite();
    fixture.requestor.fetchAndSaveFromRemoteAsync();
    CompletableFuture<Void> secondWrite = store.holdNextWrite();
    CompletableFuture<Void> secondFetch = fixture.requestor.fetchAndSaveFromRemoteAsync();
    secondWrite.completeExceptionally(new IllegalStateException("second write failed"));
    assertThrows(ExecutionException.class, () -> await(secondFetch));

    initial.complete(fixture.cached);
    assertFalse(accepted.isDone(), "The other remote save still owns persistence");
    firstWrite.completeExceptionally(new IllegalStateException("first write failed"));

    assertTrue(await(accepted));
    assertSame(fixture.cached, store.current);
    assertArrayEquals(new byte[] {2}, store.disk);
    assertEquals(3, store.submitted.size());
  }

  @Test
  void cancellingInitialResultDoesNotReleaseInitialPersistenceOwnership() throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Void> initialWrite = store.holdNextWrite();
    CompletableFuture<Boolean> accepted =
        fixture.requestor.setInitialConfiguration(
            CompletableFuture.completedFuture(fixture.cached));
    assertTrue(accepted.cancel(true));

    CompletableFuture<Void> fetched = fixture.requestor.fetchAndSaveFromRemoteAsync();
    assertEquals(1, store.submitted.size(), "Remote persistence waits for admitted initial save");
    initialWrite.complete(null);
    await(fetched);

    assertSame(fixture.remote, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
    assertEquals(2, store.submitted.size());
  }

  @Test
  void synchronousRemoteStoreFailureReleasesReservationForCacheFallback() throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    CompletableFuture<Boolean> accepted = fixture.requestor.setInitialConfiguration(initial);
    store.nextSynchronousFailure = new IllegalStateException("store rejected remote write");
    assertThrows(
        ExecutionException.class, () -> await(fixture.requestor.fetchAndSaveFromRemoteAsync()));

    initial.complete(fixture.cached);

    assertTrue(await(accepted));
    assertSame(fixture.cached, store.current);
    assertArrayEquals(new byte[] {2}, store.disk);
    assertEquals(2, store.submitted.size());
  }

  @Test
  void completedInitialFutureCannotReentrantlyRegisterASecondInitialFuture() throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture = new Fixture(store);
    store.beforeSaveReturns =
        () ->
            assertThrows(
                IllegalStateException.class,
                () ->
                    fixture.requestor.setInitialConfiguration(
                        CompletableFuture.completedFuture(fixture.remote)));

    assertTrue(
        await(
            fixture.requestor.setInitialConfiguration(
                CompletableFuture.completedFuture(fixture.cached))));

    assertSame(fixture.cached, store.current);
    assertArrayEquals(new byte[] {2}, store.disk);
    assertEquals(1, store.submitted.size());
  }

  @Test
  void reentrantRemoteFetchWaitsForInitialSaveWithoutBlockingStoreCallback() throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Void> initialWrite = store.holdNextWrite();
    AtomicReference<CompletableFuture<Void>> fetched = new AtomicReference<>();
    store.beforeSaveReturns = () -> fetched.set(fixture.requestor.fetchAndSaveFromRemoteAsync());

    CompletableFuture<Boolean> accepted =
        fixture.requestor.setInitialConfiguration(
            CompletableFuture.completedFuture(fixture.cached));

    assertNotNull(fetched.get(), "The store callback returned without waiting on its own write");
    assertFalse(fetched.get().isDone());
    assertEquals(1, store.submitted.size());
    initialWrite.complete(null);
    assertTrue(await(accepted));
    await(fetched.get());
    assertSame(fixture.remote, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
    assertEquals(2, store.submitted.size());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void synchronousFetchCoordinatesWithLateInitialConfiguration(boolean failRemote)
      throws Exception {
    TestStore store = new TestStore(true);
    Fixture fixture = new Fixture(store);
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    CompletableFuture<Boolean> accepted = fixture.requestor.setInitialConfiguration(initial);
    CompletableFuture<Void> remoteWrite = store.holdNextWrite();
    CompletableFuture<Void> saveEntered = new CompletableFuture<>();
    store.beforeSaveReturns = () -> saveEntered.complete(null);
    ExecutorService worker = Executors.newSingleThreadExecutor();

    try {
      Future<?> fetched = worker.submit(fixture.requestor::fetchAndSaveFromRemote);
      await(saveEntered);
      assertFalse(fetched.isDone());
      initial.complete(fixture.cached);

      if (failRemote) {
        remoteWrite.completeExceptionally(new IllegalStateException("remote write failed"));
        assertThrows(
            ExecutionException.class, () -> fetched.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue(await(accepted));
        assertSame(fixture.cached, store.current);
        assertArrayEquals(new byte[] {2}, store.disk);
      } else {
        remoteWrite.complete(null);
        fetched.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(await(accepted));
        assertSame(fixture.remote, store.current);
        assertArrayEquals(new byte[] {1}, store.disk);
      }
    } finally {
      remoteWrite.completeExceptionally(new IllegalStateException("test finished"));
      worker.shutdownNow();
      assertTrue(worker.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }
  }

  @Test
  void restartedFetchDiscardsLateResponseBeforeStoreAdmission() throws Exception {
    TestStore store = new TestStore(false);
    CompletableFuture<EppoConfigurationResponse> oldResponse = new CompletableFuture<>();
    CompletableFuture<EppoConfigurationResponse> newResponse = new CompletableFuture<>();
    Deque<CompletableFuture<EppoConfigurationResponse>> responses = new ArrayDeque<>();
    responses.add(oldResponse);
    responses.add(newResponse);
    Fixture fixture = new Fixture(store, request -> responses.remove());

    CompletableFuture<Void> oldFetch = fixture.requestor.fetchAndSaveFromRemoteAsync();
    assertFalse(oldFetch.isDone());
    assertTrue(store.submitted.isEmpty());
    CompletableFuture<Void> newFetch = fixture.requestor.fetchAndSaveFromRemoteAsync();
    assertTrue(oldFetch.isCancelled(), "Restarting cancels a fetch still awaiting its response");
    assertTrue(store.submitted.isEmpty());

    newResponse.complete(EppoConfigurationResponse.success(200, "new", new byte[] {1}));
    await(newFetch);
    assertTrue(oldResponse.complete(EppoConfigurationResponse.success(200, "old", new byte[] {3})));

    assertSame(fixture.remote, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
    assertEquals(1, store.submitted.size(), "The cancelled response must never reach persistence");
  }

  @Test
  void firstNotModifiedResponseDoesNotPreventInitialConfigurationFallback() throws Exception {
    TestStore store = new TestStore(false);
    Fixture fixture =
        new Fixture(
            store,
            request ->
                CompletableFuture.completedFuture(EppoConfigurationResponse.notModified("remote")));
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    CompletableFuture<Boolean> accepted = fixture.requestor.setInitialConfiguration(initial);

    await(fixture.requestor.fetchAndSaveFromRemoteAsync());
    assertTrue(store.submitted.isEmpty());
    initial.complete(fixture.cached);

    assertTrue(await(accepted));
    assertSame(fixture.cached, store.current);
    assertArrayEquals(new byte[] {2}, store.disk);
    assertEquals(1, store.submitted.size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void abstractStoreSubscriberCanStartRemoteFetchAndReceiveBothNotifications() throws Exception {
    Configuration cached = mock(Configuration.class);
    Configuration remote = mock(Configuration.class);
    List<Configuration> persisted = new ArrayList<>();
    List<Configuration> notifications = new ArrayList<>();
    AtomicReference<Configuration> current = new AtomicReference<>(Configuration.emptyConfig());
    AtomicReference<byte[]> disk = new AtomicReference<>();
    AbstractConfigurationStore<Configuration> store =
        new AbstractConfigurationStore<Configuration>() {
          @Override
          public Configuration getConfiguration() {
            return current.get();
          }

          @Override
          protected CompletableFuture<Void> persist(Configuration configuration) {
            current.set(configuration);
            disk.set(configuration == remote ? new byte[] {1} : new byte[] {2});
            persisted.add(configuration);
            return CompletableFuture.completedFuture(null);
          }
        };
    ConfigurationParser<Configuration, Object> parser = mock(ConfigurationParser.class);
    when(parser.parseFlagConfig(any())).thenReturn(mock(FlagConfigResponse.class));
    when(parser.buildConfig(any(), any(), any(), any())).thenReturn(remote);
    EppoConfigurationRequestor<Configuration, Object> requestor =
        new EppoConfigurationRequestor<>(
            store,
            false,
            parser,
            request ->
                CompletableFuture.completedFuture(
                    EppoConfigurationResponse.success(200, "remote", new byte[] {1})),
            new EppoConfigurationRequestFactory("https://example.invalid", "test", "test", "1"));
    AtomicReference<CompletableFuture<Void>> fetched = new AtomicReference<>();
    store.subscribe(
        configuration -> {
          notifications.add(configuration);
          if (configuration == cached) {
            fetched.set(requestor.fetchAndSaveFromRemoteAsync());
          }
        });

    assertTrue(await(requestor.setInitialConfiguration(CompletableFuture.completedFuture(cached))));
    assertNotNull(fetched.get());
    await(fetched.get());

    assertEquals(Arrays.asList(cached, remote), persisted);
    assertEquals(Arrays.asList(cached, remote), notifications);
    assertSame(remote, store.getConfiguration());
    assertArrayEquals(new byte[] {1}, disk.get());
  }

  @Test
  void nullAndEmptyInitialValuesDoNotSave() throws Exception {
    for (Configuration initial : new Configuration[] {null, Configuration.emptyConfig()}) {
      TestStore store = new TestStore(true);
      Fixture fixture = new Fixture(store);
      assertFalse(
          await(
              fixture.requestor.setInitialConfiguration(
                  CompletableFuture.completedFuture(initial))));
      assertTrue(store.submitted.isEmpty());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void remoteSavesWaitingForInitialPersistenceKeepSubmissionOrder(boolean failOlder)
      throws Exception {
    TestStore store = new TestStore(true);
    Configuration cached = mock(Configuration.class);
    Configuration first = mock(Configuration.class);
    Configuration second = mock(Configuration.class);
    when(cached.toString()).thenReturn("cache");
    when(first.toString()).thenReturn("remote-A");
    when(second.toString()).thenReturn("remote-B");
    store.remote = second;
    @SuppressWarnings("unchecked")
    ConfigurationParser<Configuration, Object> parser = mock(ConfigurationParser.class);
    when(parser.parseFlagConfig(any())).thenReturn(mock(FlagConfigResponse.class));
    when(parser.buildConfig(any(), any(), any(), any())).thenReturn(first, second);
    EppoConfigurationRequestor<Configuration, Object> requestor =
        new EppoConfigurationRequestor<>(
            store,
            false,
            parser,
            request ->
                CompletableFuture.completedFuture(
                    EppoConfigurationResponse.success(200, "remote", new byte[] {1})),
            new EppoConfigurationRequestFactory("https://example.invalid", "test", "test", "1"));
    CompletableFuture<Void> initialWrite = store.holdNextWrite();
    CompletableFuture<Boolean> accepted =
        requestor.setInitialConfiguration(CompletableFuture.completedFuture(cached));
    CompletableFuture<Void> olderWrite = store.holdNextWrite();
    CompletableFuture<Void> older = requestor.fetchAndSaveFromRemoteAsync();
    CompletableFuture<Void> newer = requestor.fetchAndSaveFromRemoteAsync();
    assertTrue(older.isCancelled());
    initialWrite.complete(null);
    assertTrue(await(accepted));
    assertEquals(Arrays.asList(cached, first, second), store.submitted);
    assertFalse(olderWrite.isDone());
    assertFalse(newer.isDone(), "The store still owns the pending remote writes");
    if (failOlder) {
      olderWrite.completeExceptionally(new IllegalStateException("older remote write failed"));
    } else {
      olderWrite.complete(null);
    }
    await(newer);
    assertSame(second, store.current, "The newer remote response must remain active");
    assertArrayEquals(new byte[] {1}, store.disk);
  }

  @Test
  @SuppressWarnings("unchecked")
  void remoteArrivingDuringDeferredDrainKeepsAdmissionOrder() throws Exception {
    TestStore store = new TestStore(true);
    Configuration cached = mock(Configuration.class);
    Configuration first = mock(Configuration.class);
    Configuration second = mock(Configuration.class);
    Configuration third = mock(Configuration.class);
    store.remote = third;
    ConfigurationParser<Configuration, Object> parser = mock(ConfigurationParser.class);
    when(parser.parseFlagConfig(any())).thenReturn(mock(FlagConfigResponse.class));
    when(parser.buildConfig(any(), any(), any(), any())).thenReturn(first, second, third);
    EppoConfigurationRequestor<Configuration, Object> requestor =
        new EppoConfigurationRequestor<>(
            store,
            false,
            parser,
            request ->
                CompletableFuture.completedFuture(
                    EppoConfigurationResponse.success(200, "remote", new byte[] {1})),
            new EppoConfigurationRequestFactory("https://example.invalid", "test", "test", "1"));
    CompletableFuture<Void> initialWrite = store.holdNextWrite();
    CompletableFuture<Boolean> accepted =
        requestor.setInitialConfiguration(CompletableFuture.completedFuture(cached));
    requestor.fetchAndSaveFromRemoteAsync();
    requestor.fetchAndSaveFromRemoteAsync();
    AtomicReference<CompletableFuture<Void>> reentrant = new AtomicReference<>();
    store.beforeSaveReturns = () -> reentrant.set(requestor.fetchAndSaveFromRemoteAsync());

    initialWrite.complete(null);
    assertTrue(await(accepted));
    assertNotNull(reentrant.get());
    await(reentrant.get());

    assertEquals(Arrays.asList(cached, first, second, third), store.submitted);
    assertSame(third, store.current);
    assertArrayEquals(new byte[] {1}, store.disk);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @SuppressWarnings("unchecked")
  void pendingBanditRemoteSaveCoordinatesWithLateInitialConfiguration(boolean failRemote)
      throws Exception {
    TestStore store = new TestStore(true);
    Configuration remote = mock(Configuration.class), cached = mock(Configuration.class);
    store.remote = remote;
    cloud.eppo.api.dto.BanditReference reference =
        new cloud.eppo.api.dto.BanditReference.Default("1", null);
    cloud.eppo.api.dto.BanditParameters model = mock(cloud.eppo.api.dto.BanditParameters.class);
    when(model.getModelVersion()).thenReturn("1");
    when(remote.getBanditParameters("bandit")).thenReturn(model);
    FlagConfigResponse flags = mock(FlagConfigResponse.class);
    when(flags.getBanditReferences())
        .thenReturn(java.util.Collections.singletonMap("bandit", reference));
    ConfigurationParser<Configuration, Object> parser = mock(ConfigurationParser.class);
    when(parser.parseFlagConfig(any())).thenReturn(flags);
    when(parser.buildConfig(any(), any(), any(), any())).thenReturn(remote);
    Deque<EppoConfigurationResponse> responses = new ArrayDeque<>();
    responses.add(EppoConfigurationResponse.success(200, "remote", new byte[] {1}));
    responses.add(EppoConfigurationResponse.success(200, null, new byte[] {3}));
    EppoConfigurationRequestor<Configuration, Object> requestor =
        new EppoConfigurationRequestor<>(
            store,
            true,
            parser,
            request -> CompletableFuture.completedFuture(responses.remove()),
            new EppoConfigurationRequestFactory("https://example.invalid", "test", "test", "1"));
    CompletableFuture<Void> remoteWrite = store.holdNextWrite();
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    CompletableFuture<Boolean> accepted = requestor.setInitialConfiguration(initial);
    CompletableFuture<Void> fetched = requestor.fetchAndSaveFromRemoteAsync();
    assertTrue(responses.isEmpty(), "Both flags and bandits must be fetched");
    assertEquals(1, store.submitted.size());
    assertFalse(fetched.isDone());
    initial.complete(cached);
    assertEquals(1, store.submitted.size(), "Pending bandit save owns precedence");
    if (failRemote) {
      remoteWrite.completeExceptionally(new IllegalStateException("bandit save failed"));
      assertThrows(ExecutionException.class, () -> await(fetched));
      assertTrue(await(accepted));
      assertSame(cached, store.current);
      assertArrayEquals(new byte[] {2}, store.disk);
    } else {
      remoteWrite.complete(null);
      await(fetched);
      assertFalse(await(accepted));
      assertSame(remote, store.current);
      assertArrayEquals(new byte[] {1}, store.disk);
    }
  }

  private static <T> T await(CompletableFuture<T> future) throws Exception {
    return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  private static class Fixture {
    final Configuration remote = mock(Configuration.class);
    final Configuration cached = mock(Configuration.class);
    final EppoConfigurationRequestor<Configuration, Object> requestor;

    Fixture(TestStore store) throws Exception {
      this(
          store,
          request ->
              CompletableFuture.completedFuture(
                  EppoConfigurationResponse.success(200, "remote", new byte[] {1})));
    }

    @SuppressWarnings("unchecked")
    Fixture(TestStore store, EppoConfigurationClient client) throws Exception {
      store.remote = remote;
      when(remote.getFlagsSnapshotId()).thenReturn("remote");
      ConfigurationParser<Configuration, Object> parser = mock(ConfigurationParser.class);
      when(parser.parseFlagConfig(any())).thenReturn(mock(FlagConfigResponse.class));
      when(parser.buildConfig(any(), any(), any(), any())).thenReturn(remote);
      requestor =
          new EppoConfigurationRequestor<>(
              store,
              false,
              parser,
              client,
              new EppoConfigurationRequestFactory("https://example.invalid", "test", "test", "1"));
    }
  }

  /** Models asynchronous persistence; a successful completion updates both memory and disk. */
  private static class TestStore implements IConfigurationStore<Configuration> {
    final List<Configuration> submitted = new ArrayList<>();
    final Deque<CompletableFuture<Void>> outcomes = new ArrayDeque<>();
    final boolean queueWrites;
    Configuration remote;
    Configuration current = Configuration.emptyConfig();
    byte[] disk;
    Runnable beforeSaveReturns;
    RuntimeException nextSynchronousFailure;
    CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);

    TestStore(boolean queueWrites) {
      this.queueWrites = queueWrites;
    }

    CompletableFuture<Void> holdNextWrite() {
      CompletableFuture<Void> outcome = new CompletableFuture<>();
      outcomes.add(outcome);
      return outcome;
    }

    @Override
    public Configuration getConfiguration() {
      return current;
    }

    @Override
    public CompletableFuture<Void> saveConfiguration(Configuration configuration) {
      submitted.add(configuration);
      RuntimeException synchronousFailure = nextSynchronousFailure;
      nextSynchronousFailure = null;
      if (synchronousFailure != null) throw synchronousFailure;
      CompletableFuture<Void> outcome =
          outcomes.isEmpty() ? CompletableFuture.completedFuture(null) : outcomes.remove();
      CompletableFuture<Void> prerequisite =
          queueWrites
              ? tail.handle((ignored, failure) -> null)
              : CompletableFuture.completedFuture(null);
      CompletableFuture<Void> result =
          prerequisite
              .thenCompose(ignored -> outcome)
              .thenRun(
                  () -> {
                    current = configuration;
                    disk = configuration == remote ? new byte[] {1} : new byte[] {2};
                  });
      tail = result;
      Runnable reentry = beforeSaveReturns;
      beforeSaveReturns = null;
      if (reentry != null) reentry.run();
      return result;
    }

    @Override
    public Runnable subscribe(Consumer<Configuration> callback) {
      return () -> {};
    }

    @Override
    public boolean unsubscribe(Consumer<Configuration> callback) {
      return false;
    }
  }
}

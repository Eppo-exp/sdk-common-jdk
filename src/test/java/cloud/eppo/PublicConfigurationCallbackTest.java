package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cloud.eppo.api.Configuration;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.http.EppoConfigurationResponse;
import cloud.eppo.parser.ConfigurationParser;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PublicConfigurationCallbackTest {
  private static final long TIMEOUT_SECONDS = 3;
  private static final AtomicInteger WORKERS = new AtomicInteger();

  @Test
  void setConfigurationReturnsWhileCustomerCallbackIsBlocked() throws Exception {
    MemoryOnlyConfigurationStore store = new MemoryOnlyConfigurationStore();
    Configuration cached = mock(Configuration.class);
    Configuration remote = mock(Configuration.class);
    Fixture fixture = new Fixture(store, null, false, remote);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    BlockingQueue<Configuration> notifications = new LinkedBlockingQueue<>();
    fixture.client.onConfigurationChange(
        config -> {
          notifications.add(config);
          if (config == cached) {
            entered.countDown();
            awaitRelease(release);
          }
        });

    try {
      CompletableFuture<Void> installed =
          runDaemon(
              () -> {
                fixture.client.setConfiguration(cached);
                return null;
              });
      assertTrue(entered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      await(installed);
      assertSame(cached, fixture.client.getConfiguration());
      await(
          runDaemon(
              () -> {
                fixture.client.loadConfiguration();
                return null;
              }));
      assertSame(remote, fixture.client.getConfiguration());
      assertSame(cached, next(notifications));
    } finally {
      release.countDown();
    }
    assertSame(remote, next(notifications));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void customerCallbackCanLoadDuringInitialNotification(boolean asyncLoad) throws Exception {
    Configuration cached = mock(Configuration.class);
    Configuration remote = mock(Configuration.class);
    MemoryOnlyConfigurationStore store = new MemoryOnlyConfigurationStore();
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    Fixture fixture = new Fixture(store, initial, false, remote);
    BlockingQueue<Configuration> notifications = new LinkedBlockingQueue<>();
    CompletableFuture<Void> reentered = new CompletableFuture<>();
    fixture.client.onConfigurationChange(
        config -> {
          notifications.add(config);
          if (config == cached) {
            completeReentry(fixture.client, asyncLoad, reentered);
          }
        });

    await(runDaemon(() -> initial.complete(cached)));
    assertTrue(await(fixture.client.getInitialConfigFuture()));
    await(reentered);
    assertSame(remote, store.getConfiguration());
    assertEquals(
        Arrays.asList(cached, remote), Arrays.asList(next(notifications), next(notifications)));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void customerCallbackCanLoadDuringDeferredRemoteNotification(boolean asyncLoad) throws Exception {
    Configuration cached = mock(Configuration.class);
    Configuration first = mock(Configuration.class);
    Configuration second = mock(Configuration.class);
    HeldInitialStore store = new HeldInitialStore(cached);
    CompletableFuture<Configuration> initial = new CompletableFuture<>();
    Fixture fixture = new Fixture(store, initial, false, first, second);
    BlockingQueue<Configuration> notifications = new LinkedBlockingQueue<>();
    CompletableFuture<Void> reentered = new CompletableFuture<>();
    fixture.client.onConfigurationChange(
        config -> {
          notifications.add(config);
          if (config == first) {
            completeReentry(fixture.client, asyncLoad, reentered);
          }
        });
    initial.complete(cached);
    CompletableFuture<Void> fetched = fixture.client.loadConfigurationAsync();
    assertFalse(fetched.isDone(), "Remote persistence waits for the initial write");

    await(runDaemon(() -> store.initialWrite.complete(null)));
    assertTrue(await(fixture.client.getInitialConfigFuture()));
    await(fetched.handle((ignored, failure) -> null));
    await(reentered);
    assertSame(second, store.getConfiguration());
    assertEquals(
        Arrays.asList(cached, first, second),
        Arrays.asList(next(notifications), next(notifications), next(notifications)));
  }

  @Test
  void customerCallbackCanSetAnotherConfiguration() throws Exception {
    Configuration first = mock(Configuration.class);
    Configuration second = mock(Configuration.class);
    Fixture fixture = new Fixture(new MemoryOnlyConfigurationStore(), null, false);
    BlockingQueue<Configuration> notifications = new LinkedBlockingQueue<>();
    CompletableFuture<Void> reentered = new CompletableFuture<>();
    fixture.client.onConfigurationChange(
        config -> {
          notifications.add(config);
          if (config == first) {
            try {
              fixture.client.setConfiguration(second);
              reentered.complete(null);
            } catch (Throwable failure) {
              reentered.completeExceptionally(failure);
            }
          }
        });

    fixture.client.setConfiguration(first);
    await(reentered);
    assertSame(second, fixture.client.getConfiguration(), "Reentrant updates must not be dropped");
    assertEquals(
        Arrays.asList(first, second), Arrays.asList(next(notifications), next(notifications)));
  }

  @Test
  void callbackRetainsItsConfigurationWhenActiveConfigurationAdvances() throws Exception {
    Configuration first = mock(Configuration.class);
    Configuration second = mock(Configuration.class);
    Fixture fixture = new Fixture(new MemoryOnlyConfigurationStore(), null, false);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CompletableFuture<Configuration[]> observed = new CompletableFuture<>();
    fixture.client.onConfigurationChange(
        config -> {
          if (config == first) {
            entered.countDown();
            awaitRelease(release);
            observed.complete(new Configuration[] {config, fixture.client.getConfiguration()});
          }
        });

    try {
      CompletableFuture<Void> installed =
          runDaemon(
              () -> {
                fixture.client.setConfiguration(first);
                return null;
              });
      assertTrue(entered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      await(installed);
      await(
          runDaemon(
              () -> {
                fixture.client.setConfiguration(second);
                return null;
              }));
    } finally {
      release.countDown();
    }
    Configuration[] result = await(observed);
    assertSame(first, result[0], "The callback keeps the configuration supplied for its event");
    assertSame(second, result[1], "A later getter may already expose a newer configuration");
  }

  @Test
  void blockedCallbackDoesNotBlockAnotherClient() throws Exception {
    Fixture first = new Fixture(new MemoryOnlyConfigurationStore(), null, false);
    Fixture second = new Fixture(new MemoryOnlyConfigurationStore(), null, false);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CompletableFuture<Configuration> delivered = new CompletableFuture<>();
    first.client.onConfigurationChange(
        config -> {
          entered.countDown();
          awaitRelease(release);
        });
    second.client.onConfigurationChange(delivered::complete);
    Configuration firstConfig = mock(Configuration.class);
    Configuration secondConfig = mock(Configuration.class);

    try {
      CompletableFuture<Void> firstInstalled =
          runDaemon(
              () -> {
                first.client.setConfiguration(firstConfig);
                return null;
              });
      assertTrue(entered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      second.client.setConfiguration(secondConfig);
      assertSame(secondConfig, await(delivered));
      release.countDown();
      await(firstInstalled);
    } finally {
      release.countDown();
    }
  }

  @Test
  void throwingCustomerCallbackDoesNotFailOtherObserverOrSave() throws Exception {
    Fixture fixture = new Fixture(new MemoryOnlyConfigurationStore(), null, false);
    BlockingQueue<Configuration> notifications = new LinkedBlockingQueue<>();
    fixture.client.onConfigurationChange(
        config -> {
          throw new IllegalStateException("Customer callback failed");
        });
    fixture.client.onConfigurationChange(notifications::add);
    Configuration first = mock(Configuration.class);
    Configuration second = mock(Configuration.class);

    assertDoesNotThrow(() -> fixture.client.setConfiguration(first));
    assertSame(first, next(notifications));
    assertDoesNotThrow(() -> fixture.client.setConfiguration(second));
    assertSame(second, next(notifications));
    assertSame(second, fixture.client.getConfiguration());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void failedSaveDoesNotPublishAChange(boolean graceful) throws Exception {
    FailFirstStore store = new FailFirstStore();
    Fixture fixture = new Fixture(store, null, graceful);
    BlockingQueue<Configuration> notifications = new LinkedBlockingQueue<>();
    fixture.client.onConfigurationChange(notifications::add);
    Configuration failed = mock(Configuration.class);
    Configuration successful = mock(Configuration.class);

    if (graceful) {
      assertDoesNotThrow(() -> fixture.client.setConfiguration(failed));
    } else {
      assertThrows(CompletionException.class, () -> fixture.client.setConfiguration(failed));
    }
    assertNotSame(failed, store.getConfiguration());
    fixture.client.setConfiguration(successful);
    assertSame(successful, next(notifications), "The failed installation must not emit an event");
    assertSame(successful, store.getConfiguration());
  }

  private static void completeReentry(
      BaseEppoClient<Configuration, Object> client,
      boolean asyncLoad,
      CompletableFuture<Void> result) {
    try {
      if (asyncLoad) {
        client.loadConfigurationAsync().join();
      } else {
        client.loadConfiguration();
      }
      result.complete(null);
    } catch (Throwable failure) {
      result.completeExceptionally(failure);
    }
  }

  private static <T> CompletableFuture<T> runDaemon(Callable<T> operation) {
    CompletableFuture<T> result = new CompletableFuture<>();
    Thread worker =
        new Thread(
            () -> {
              try {
                result.complete(operation.call());
              } catch (Throwable failure) {
                result.completeExceptionally(failure);
              }
            },
            "public-callback-regression-" + WORKERS.incrementAndGet());
    worker.setDaemon(true);
    worker.start();
    return result;
  }

  private static void awaitRelease(CountDownLatch release) {
    try {
      if (!release.await(TIMEOUT_SECONDS * 3, TimeUnit.SECONDS)) {
        throw new AssertionError("The test did not release its customer callback");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new AssertionError(interrupted);
    }
  }

  private static <T> T await(CompletableFuture<T> result) throws Exception {
    return result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  private static Configuration next(BlockingQueue<Configuration> notifications)
      throws InterruptedException {
    Configuration result = notifications.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertNotNull(result, "Expected a customer configuration notification");
    return result;
  }

  private static final class Fixture {
    final BaseEppoClient<Configuration, Object> client;

    @SuppressWarnings("unchecked")
    Fixture(
        IConfigurationStore<Configuration> store,
        CompletableFuture<Configuration> initial,
        boolean graceful,
        Configuration... remoteConfigurations)
        throws Exception {
      ConfigurationParser<Configuration, Object> parser = mock(ConfigurationParser.class);
      when(parser.parseFlagConfig(any())).thenReturn(mock(FlagConfigResponse.class));
      AtomicInteger remoteIndex = new AtomicInteger();
      when(parser.buildConfig(any(), any(), any(), any()))
          .thenAnswer(
              ignored -> {
                int index = remoteIndex.getAndIncrement();
                if (index >= remoteConfigurations.length) {
                  throw new AssertionError("Unexpected extra remote configuration");
                }
                return remoteConfigurations[index];
              });
      client =
          new BaseEppoClient<>(
              "test-api-key",
              "test-sdk",
              "1.0.0",
              null,
              null,
              null,
              store,
              graceful,
              false,
              false,
              initial,
              null,
              null,
              parser,
              request ->
                  CompletableFuture.completedFuture(
                      EppoConfigurationResponse.success(200, "remote", new byte[] {1})));
    }
  }

  private static final class HeldInitialStore extends AbstractConfigurationStore<Configuration> {
    final Configuration cached;
    final CompletableFuture<Void> initialWrite = new CompletableFuture<>();
    final AtomicReference<Configuration> current =
        new AtomicReference<>(Configuration.emptyConfig());

    HeldInitialStore(Configuration cached) {
      this.cached = cached;
    }

    @Override
    public Configuration getConfiguration() {
      return current.get();
    }

    @Override
    protected CompletableFuture<Void> persist(Configuration configuration) {
      if (configuration == cached) {
        return initialWrite.thenRun(() -> current.set(configuration));
      }
      current.set(configuration);
      return CompletableFuture.completedFuture(null);
    }
  }

  private static final class FailFirstStore extends AbstractConfigurationStore<Configuration> {
    final AtomicBoolean failNext = new AtomicBoolean(true);
    final AtomicReference<Configuration> current =
        new AtomicReference<>(Configuration.emptyConfig());

    @Override
    public Configuration getConfiguration() {
      return current.get();
    }

    @Override
    protected CompletableFuture<Void> persist(Configuration configuration) {
      if (failNext.getAndSet(false)) {
        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("Persistence failed"));
        return failed;
      }
      current.set(configuration);
      return CompletableFuture.completedFuture(null);
    }
  }
}

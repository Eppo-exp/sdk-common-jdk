package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import cloud.eppo.api.Configuration;
import cloud.eppo.api.EppoActionCallback;
import cloud.eppo.helpers.TestUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ConfigurationRequestorTest {
  private final File initialFlagConfigFile =
      new File("src/test/resources/static/initial-flag-config.json");
  private final File differentFlagConfigFile =
      new File("src/test/resources/static/boolean-flag.json");

  private ConfigurationStore mockConfigStore;
  private IEppoHttpClient mockHttpClient;
  private ConfigurationRequestor requestor;

  @BeforeEach
  public void setup() {
    mockConfigStore = mock(ConfigurationStore.class);
    mockHttpClient = mock(EppoHttpClient.class);
    requestor = new ConfigurationRequestor(mockConfigStore, mockHttpClient, false, true);
  }

  @Test
  public void testBrokenFetchDoesntClobberCache() throws IOException, InterruptedException {
    IConfigurationStore configStore = Mockito.spy(new ConfigurationStore());
    TestUtils.DelayedHttpClient mockHttpClient = new TestUtils.DelayedHttpClient("".getBytes());

    ConfigurationRequestor requestor =
        new ConfigurationRequestor(configStore, mockHttpClient, false, true);

    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);

    // Set initial config and verify that it has been set.
    requestor.setInitialConfiguration(Configuration.builder(flagConfig.getBytes()).build());
    assertFalse(configStore.getConfiguration().isEmpty());
    Mockito.verify(configStore, Mockito.times(1)).saveConfiguration(any());

    CountDownLatch latch = new CountDownLatch(1);
    requestor.fetchAndSaveFromRemoteAsync(
        new EppoActionCallback<Configuration>() {

          @Override
          public void onSuccess(Configuration data) {
            fail("Unexpected success");
          }

          @Override
          public void onFailure(Throwable error) {
            latch.countDown();
          }
        });

    // Error out the fetch
    mockHttpClient.fail(new Exception("Intentional exception"));

    assertFalse(configStore.getConfiguration().isEmpty());
    Mockito.verify(configStore, Mockito.times(1)).saveConfiguration(any());

    // `numeric_flag` is only in the cache which should be available
    assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));

    assertNull(configStore.getConfiguration().getFlag("boolean_flag"));

    // Ensure fetch failure callback is called
    assertTrue(latch.await(1, TimeUnit.SECONDS));
  }

  @Test
  public void testCacheWritesAfterBrokenFetch() throws IOException, InterruptedException {
    IConfigurationStore configStore = Mockito.spy(new ConfigurationStore());
    TestUtils.DelayedHttpClient mockHttpClient = new TestUtils.DelayedHttpClient("".getBytes());

    ConfigurationRequestor requestor =
        new ConfigurationRequestor(configStore, mockHttpClient, false, true);

    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    Mockito.verify(configStore, Mockito.times(0)).saveConfiguration(any());
    assertTrue(configStore.getConfiguration().isEmpty());

    CountDownLatch latch = new CountDownLatch(1);
    requestor.fetchAndSaveFromRemoteAsync(
        new EppoActionCallback<Configuration>() {

          @Override
          public void onSuccess(Configuration data) {
            fail("Unexpected success");
          }

          @Override
          public void onFailure(Throwable error) {
            latch.countDown();
          }
        });

    // Error out the fetch
    mockHttpClient.fail(new Exception("Intentional exception"));

    // Set initial config and verify that it has been set.
    requestor.setInitialConfiguration(Configuration.builder(flagConfig.getBytes()).build());

    Mockito.verify(configStore, Mockito.times(1)).saveConfiguration(any());
    assertFalse(configStore.getConfiguration().isEmpty());

    // `numeric_flag` is only in the cache which should be available
    assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));

    assertNull(configStore.getConfiguration().getFlag("boolean_flag"));

    // Ensure fetch failure callback is called
    assertTrue(latch.await(1, TimeUnit.SECONDS));
  }

  static class ListAddingConfigCallback implements Configuration.ConfigurationCallback {
    public final List<Configuration> results = new ArrayList<>();

    @Override
    public void accept(Configuration configuration) {
      results.add(configuration);
    }
  }

  @Test
  public void testConfigurationChangeListener() throws IOException {
    // Setup mock response
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    when(mockHttpClient.get(anyString())).thenReturn(flagConfig.getBytes());

    ListAddingConfigCallback receivedConfigs = new ListAddingConfigCallback();

    // Subscribe to configuration changes
    Runnable unsubscribe = requestor.onConfigurationChange(receivedConfigs);

    // Initial fetch should trigger the callback
    requestor.fetchAndSaveFromRemote();
    assertEquals(1, receivedConfigs.results.size());

    // Another fetch should trigger the callback again (fetches aren't optimized with eTag yet).
    requestor.fetchAndSaveFromRemote();
    assertEquals(2, receivedConfigs.results.size());

    // Unsubscribe should prevent further callbacks
    unsubscribe.run();
    requestor.fetchAndSaveFromRemote();
    assertEquals(2, receivedConfigs.results.size()); // Count should remain the same
  }

  @Test
  public void testMultipleConfigurationChangeListeners() {
    // Setup mock response
    when(mockHttpClient.get(anyString())).thenReturn("{}".getBytes());

    AtomicInteger callCount1 = new AtomicInteger(0);
    AtomicInteger callCount2 = new AtomicInteger(0);

    // Subscribe multiple listeners
    Runnable unsubscribe1 =
        requestor.onConfigurationChange(
            new Configuration.ConfigurationCallback() {
              @Override
              public void accept(Configuration configuration) {
                callCount1.incrementAndGet();
              }
            });
    Runnable unsubscribe2 =
        requestor.onConfigurationChange(
            new Configuration.ConfigurationCallback() {
              @Override
              public void accept(Configuration configuration) {
                callCount2.incrementAndGet();
              }
            });

    // Fetch should trigger both callbacks
    requestor.fetchAndSaveFromRemote();
    assertEquals(1, callCount1.get());
    assertEquals(1, callCount2.get());

    // Unsubscribe first listener
    unsubscribe1.run();
    requestor.fetchAndSaveFromRemote();
    assertEquals(1, callCount1.get()); // Should not increase
    assertEquals(2, callCount2.get()); // Should increase

    // Unsubscribe second listener
    unsubscribe2.run();
    requestor.fetchAndSaveFromRemote();
    assertEquals(1, callCount1.get()); // Should not increase
    assertEquals(2, callCount2.get()); // Should not increase
  }

  @Test
  public void testConfigurationChangeListenerIgnoresFailedFetch() {
    // throw on get
    when(mockHttpClient.get(anyString())).thenThrow(new RuntimeException("fetch failed"));

    AtomicInteger callCount = new AtomicInteger(0);
    requestor.onConfigurationChange(v -> callCount.incrementAndGet());

    // Failed save should not trigger the callback
    try {
      requestor.fetchAndSaveFromRemote();
    } catch (RuntimeException e) {
      // Pass
    }
    assertEquals(0, callCount.get());
  }

  @Test
  public void testConfigurationChangeListenerIgnoresFailedSave() {
    // throw on get
    when(mockHttpClient.get(anyString())).thenReturn("{}".getBytes());
    doThrow(new RuntimeException("Save failed"))
        .when(mockConfigStore)
        .saveConfiguration(any(Configuration.class));

    AtomicInteger callCount = new AtomicInteger(0);
    requestor.onConfigurationChange(v -> callCount.incrementAndGet());

    // Failed save should not trigger the callback
    try {
      requestor.fetchAndSaveFromRemote();
    } catch (RuntimeException e) {
      // Pass
    }
    assertEquals(0, callCount.get());
  }

  @Test
  public void testConfigurationChangeListenerAsyncSave() throws InterruptedException {
    // Setup mock responses
    TestUtils.DelayedHttpClient mockHttpClient =
        new TestUtils.DelayedHttpClient("{\"flags\":{}}".getBytes());
    requestor = new ConfigurationRequestor(mockConfigStore, mockHttpClient, false, true);

    CountDownLatch countDownLatch = new CountDownLatch(1);

    requestor.onConfigurationChange(v -> countDownLatch.countDown());

    // Start fetch
    requestor.fetchAndSaveFromRemoteAsync(
        new EppoActionCallback<Configuration>() {
          @Override
          public void onSuccess(Configuration data) {
            // This callback should be notified after the registered listeners have been notified.
            assertEquals(0, countDownLatch.getCount());
          }

          @Override
          public void onFailure(Throwable error) {
            fail("unexpected failure");
          }
        });

    assertEquals(1, countDownLatch.getCount()); // Fetch not yet completed

    // Complete the save
    mockHttpClient.flush();

    assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));
    assertEquals(0, countDownLatch.getCount()); // Callback should be called after save completes
  }
}

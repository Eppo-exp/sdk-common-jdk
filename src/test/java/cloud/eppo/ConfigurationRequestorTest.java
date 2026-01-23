package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.eppo.api.Configuration;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ConfigurationRequestorTest {
  private final File initialFlagConfigFile =
      new File("src/test/resources/static/initial-flag-config.json");
  private final File differentFlagConfigFile =
      new File("src/test/resources/static/boolean-flag.json");

  @Test
  public void testInitialConfigurationFuture() throws IOException {
    IConfigurationStore configStore = Mockito.spy(new ConfigurationStore());
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    ConfigurationRequestor requestor =
        new ConfigurationRequestor(configStore, mockHttpClient, false, true);

    CompletableFuture<Configuration> futureConfig = new CompletableFuture<>();
    byte[] flagConfig = FileUtils.readFileToByteArray(initialFlagConfigFile);

    requestor.setInitialConfiguration(futureConfig);

    // verify config is empty to start
    assertTrue(configStore.getConfiguration().isEmpty());
    assertEquals(Collections.emptySet(), configStore.getConfiguration().getFlagKeys());
    Mockito.verify(configStore, Mockito.times(0)).saveConfiguration(any());

    futureConfig.complete(Configuration.builder(flagConfig, null).build());

    assertFalse(configStore.getConfiguration().isEmpty());
    assertFalse(configStore.getConfiguration().getFlagKeys().isEmpty());
    Mockito.verify(configStore, Mockito.times(1)).saveConfiguration(any());
    assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));
  }

  @Test
  public void testInitialConfigurationDoesntClobberFetch() throws IOException {
    IConfigurationStore configStore = Mockito.spy(new ConfigurationStore());
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    ConfigurationRequestor requestor =
        new ConfigurationRequestor(configStore, mockHttpClient, false, true);

    CompletableFuture<Configuration> initialConfigFuture = new CompletableFuture<>();
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    CompletableFuture<byte[]> configFetchFuture = new CompletableFuture<>();
    String fetchedFlagConfig =
        FileUtils.readFileToString(differentFlagConfigFile, StandardCharsets.UTF_8);

    when(mockHttpClient.getAsync("/flag-config/v1/config")).thenReturn(configFetchFuture);

    // Set initial config and verify that no config has been set yet.
    requestor.setInitialConfiguration(initialConfigFuture);

    assertTrue(configStore.getConfiguration().isEmpty());
    assertEquals(Collections.emptySet(), configStore.getConfiguration().getFlagKeys());
    Mockito.verify(configStore, Mockito.times(0)).saveConfiguration(any());

    // The initial config contains only one flag keyed `numeric_flag`. The fetch response has only
    // one flag keyed
    // `boolean_flag`. We make sure to complete the fetch future first to verify the cache load does
    // not overwrite it.
    CompletableFuture<Void> handle = requestor.fetchAndSaveFromRemoteAsync();

    // Resolve the fetch and then the initialConfig
    configFetchFuture.complete(fetchedFlagConfig.getBytes(StandardCharsets.UTF_8));
    initialConfigFuture.complete(new Configuration.Builder(flagConfig.getBytes()).build());

    assertFalse(configStore.getConfiguration().isEmpty());
    assertFalse(configStore.getConfiguration().getFlagKeys().isEmpty());
    Mockito.verify(configStore, Mockito.times(1)).saveConfiguration(any());

    // `numeric_flag` is only in the cache which should have been ignored.
    assertNull(configStore.getConfiguration().getFlag("numeric_flag"));

    // `boolean_flag` is available only from the fetch
    assertNotNull(configStore.getConfiguration().getFlag("boolean_flag"));
  }

  @Test
  public void testBrokenFetchDoesntClobberCache() throws IOException {
    IConfigurationStore configStore = Mockito.spy(new ConfigurationStore());
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    ConfigurationRequestor requestor =
        new ConfigurationRequestor(configStore, mockHttpClient, false, true);

    CompletableFuture<Configuration> initialConfigFuture = new CompletableFuture<>();
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    CompletableFuture<byte[]> configFetchFuture = new CompletableFuture<>();

    when(mockHttpClient.getAsync("/flag-config/v1/config")).thenReturn(configFetchFuture);

    // Set initial config and verify that no config has been set yet.
    requestor.setInitialConfiguration(initialConfigFuture);

    assertTrue(configStore.getConfiguration().isEmpty());
    assertEquals(Collections.emptySet(), configStore.getConfiguration().getFlagKeys());
    Mockito.verify(configStore, Mockito.times(0)).saveConfiguration(any());

    requestor.fetchAndSaveFromRemoteAsync();

    // Resolve the initial config
    initialConfigFuture.complete(new Configuration.Builder(flagConfig.getBytes()).build());

    // Error out the fetch
    configFetchFuture.completeExceptionally(new Exception("Intentional exception"));

    assertFalse(configStore.getConfiguration().isEmpty());
    assertFalse(configStore.getConfiguration().getFlagKeys().isEmpty());
    Mockito.verify(configStore, Mockito.times(1)).saveConfiguration(any());

    // `numeric_flag` is only in the cache which should be available
    assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));

    assertNull(configStore.getConfiguration().getFlag("boolean_flag"));
  }

  @Test
  public void testCacheWritesAfterBrokenFetch() throws IOException {
    IConfigurationStore configStore = Mockito.spy(new ConfigurationStore());
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    ConfigurationRequestor requestor =
        new ConfigurationRequestor(configStore, mockHttpClient, false, true);

    CompletableFuture<Configuration> initialConfigFuture = new CompletableFuture<>();
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    CompletableFuture<byte[]> configFetchFuture = new CompletableFuture<>();

    when(mockHttpClient.getAsync("/flag-config/v1/config")).thenReturn(configFetchFuture);

    // Set initial config and verify that no config has been set yet.
    requestor.setInitialConfiguration(initialConfigFuture);
    Mockito.verify(configStore, Mockito.times(0)).saveConfiguration(any());

    // default configuration is empty config.
    assertTrue(configStore.getConfiguration().isEmpty());
    assertEquals(Collections.emptySet(), configStore.getConfiguration().getFlagKeys());

    // Fetch from remote with an error
    requestor.fetchAndSaveFromRemoteAsync();
    configFetchFuture.completeExceptionally(new Exception("Intentional exception"));

    // Resolve the initial config after the fetch throws an error.
    initialConfigFuture.complete(new Configuration.Builder(flagConfig.getBytes()).build());

    // Verify that a configuration was saved by the requestor
    Mockito.verify(configStore, Mockito.times(1)).saveConfiguration(any());
    assertFalse(configStore.getConfiguration().isEmpty());
    assertFalse(configStore.getConfiguration().getFlagKeys().isEmpty());

    // `numeric_flag` is only in the cache which should be available
    assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));

    assertNull(configStore.getConfiguration().getFlag("boolean_flag"));
  }

  private ConfigurationStore mockConfigStore;
  private EppoHttpClient mockHttpClient;
  private ConfigurationRequestor requestor;

  @BeforeEach
  public void setup() {
    mockConfigStore = mock(ConfigurationStore.class);
    mockHttpClient = mock(EppoHttpClient.class);
    requestor = new ConfigurationRequestor(mockConfigStore, mockHttpClient, false, true);
  }

  @Test
  public void testConfigurationChangeListener() throws IOException {
    // Setup mock response
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    when(mockHttpClient.get(anyString())).thenReturn(flagConfig.getBytes());
    when(mockConfigStore.saveConfiguration(any()))
        .thenReturn(CompletableFuture.completedFuture(null));

    List<Configuration> receivedConfigs = new ArrayList<>();

    // Subscribe to configuration changes
    Runnable unsubscribe = requestor.onConfigurationChange(receivedConfigs::add);

    // Initial fetch should trigger the callback
    requestor.fetchAndSaveFromRemote();
    assertEquals(1, receivedConfigs.size());

    // Another fetch should trigger the callback again (fetches aren't optimized with eTag yet).
    requestor.fetchAndSaveFromRemote();
    assertEquals(2, receivedConfigs.size());

    // Unsubscribe should prevent further callbacks
    unsubscribe.run();
    requestor.fetchAndSaveFromRemote();
    assertEquals(2, receivedConfigs.size()); // Count should remain the same
  }

  @Test
  public void testMultipleConfigurationChangeListeners() {
    // Setup mock response
    when(mockHttpClient.get(anyString())).thenReturn("{}".getBytes());
    when(mockConfigStore.saveConfiguration(any()))
        .thenReturn(CompletableFuture.completedFuture(null));

    AtomicInteger callCount1 = new AtomicInteger(0);
    AtomicInteger callCount2 = new AtomicInteger(0);

    // Subscribe multiple listeners
    Runnable unsubscribe1 = requestor.onConfigurationChange(v -> callCount1.incrementAndGet());
    Runnable unsubscribe2 = requestor.onConfigurationChange(v -> callCount2.incrementAndGet());

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
    // Setup mock response to simulate failure
    when(mockHttpClient.get(anyString())).thenThrow(new RuntimeException("Fetch failed"));

    AtomicInteger callCount = new AtomicInteger(0);
    requestor.onConfigurationChange(v -> callCount.incrementAndGet());

    // Failed fetch should not trigger the callback
    try {
      requestor.fetchAndSaveFromRemote();
    } catch (Exception e) {
      // Expected
    }
    assertEquals(0, callCount.get());
  }

  @Test
  public void testConfigurationChangeListenerIgnoresFailedSave() {
    // Setup mock responses
    when(mockHttpClient.get(anyString())).thenReturn("{}".getBytes());
    when(mockConfigStore.saveConfiguration(any()))
        .thenReturn(
            CompletableFuture.supplyAsync(
                () -> {
                  throw new RuntimeException("Save failed");
                }));

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
  public void testConfigurationChangeListenerAsyncSave() {
    // Setup mock responses
    when(mockHttpClient.getAsync(anyString()))
        .thenReturn(CompletableFuture.completedFuture("{\"flags\":{}}".getBytes()));

    CompletableFuture<Void> saveFuture = new CompletableFuture<>();
    when(mockConfigStore.saveConfiguration(any())).thenReturn(saveFuture);

    AtomicInteger callCount = new AtomicInteger(0);
    requestor.onConfigurationChange(v -> callCount.incrementAndGet());

    // Start fetch
    CompletableFuture<Void> fetch = requestor.fetchAndSaveFromRemoteAsync();
    assertEquals(0, callCount.get()); // Callback should not be called yet

    // Complete the save
    saveFuture.complete(null);
    fetch.join();
    assertEquals(1, callCount.get()); // Callback should be called after save completes
  }

  @Test
  public void testUnsubscribeFromConfigurationChangeByReference() throws IOException {
    // Setup mock response
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    when(mockHttpClient.get(anyString())).thenReturn(flagConfig.getBytes());
    when(mockConfigStore.saveConfiguration(any()))
        .thenReturn(CompletableFuture.completedFuture(null));

    List<Configuration> receivedConfigs = new ArrayList<>();
    Consumer<Configuration> callback = receivedConfigs::add;

    // Subscribe to configuration changes
    requestor.onConfigurationChange(callback);

    // Initial fetch should trigger the callback
    requestor.fetchAndSaveFromRemote();
    assertEquals(1, receivedConfigs.size());

    // Unsubscribe using the callback reference
    boolean removed = requestor.unsubscribeFromConfigurationChange(callback);
    assertTrue(removed);

    // Another fetch should not trigger the callback
    requestor.fetchAndSaveFromRemote();
    assertEquals(1, receivedConfigs.size()); // Count should remain the same
  }

  @Test
  public void testUnsubscribeNonExistentConfigurationChangeListener() {
    Consumer<Configuration> callback = config -> {};

    // Try to unsubscribe a callback that was never subscribed
    boolean removed = requestor.unsubscribeFromConfigurationChange(callback);
    assertFalse(removed);
  }

  @Test
  public void testUnsubscribeOneOfMultipleConfigurationChangeListeners() {
    // Setup mock response
    when(mockHttpClient.get(anyString())).thenReturn("{}".getBytes());
    when(mockConfigStore.saveConfiguration(any()))
        .thenReturn(CompletableFuture.completedFuture(null));

    AtomicInteger callCount1 = new AtomicInteger(0);
    AtomicInteger callCount2 = new AtomicInteger(0);
    AtomicInteger callCount3 = new AtomicInteger(0);

    Consumer<Configuration> callback1 = v -> callCount1.incrementAndGet();
    Consumer<Configuration> callback2 = v -> callCount2.incrementAndGet();
    Consumer<Configuration> callback3 = v -> callCount3.incrementAndGet();

    // Subscribe multiple listeners
    requestor.onConfigurationChange(callback1);
    requestor.onConfigurationChange(callback2);
    requestor.onConfigurationChange(callback3);

    // Fetch should trigger all callbacks
    requestor.fetchAndSaveFromRemote();
    assertEquals(1, callCount1.get());
    assertEquals(1, callCount2.get());
    assertEquals(1, callCount3.get());

    // Unsubscribe middle listener
    boolean removed = requestor.unsubscribeFromConfigurationChange(callback2);
    assertTrue(removed);

    requestor.fetchAndSaveFromRemote();
    assertEquals(2, callCount1.get()); // Should increase
    assertEquals(1, callCount2.get()); // Should not increase
    assertEquals(2, callCount3.get()); // Should increase
  }
}

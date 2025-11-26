package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    futureConfig.complete(Configuration.builder(flagConfig, false).build());

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
    CompletableFuture<EppoHttpResponse> configFetchFuture = new CompletableFuture<>();
    String fetchedFlagConfig =
        FileUtils.readFileToString(differentFlagConfigFile, StandardCharsets.UTF_8);

    when(mockHttpClient.getAsync(anyString(), any())).thenReturn(configFetchFuture);

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
    configFetchFuture.complete(
        new EppoHttpResponse(fetchedFlagConfig.getBytes(StandardCharsets.UTF_8), 200, null));
    initialConfigFuture.complete(new Configuration.Builder(flagConfig, false).build());

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
    CompletableFuture<EppoHttpResponse> configFetchFuture = new CompletableFuture<>();

    when(mockHttpClient.getAsync(anyString(), any())).thenReturn(configFetchFuture);

    // Set initial config and verify that no config has been set yet.
    requestor.setInitialConfiguration(initialConfigFuture);

    assertTrue(configStore.getConfiguration().isEmpty());
    assertEquals(Collections.emptySet(), configStore.getConfiguration().getFlagKeys());
    Mockito.verify(configStore, Mockito.times(0)).saveConfiguration(any());

    requestor.fetchAndSaveFromRemoteAsync();

    // Resolve the initial config
    initialConfigFuture.complete(new Configuration.Builder(flagConfig, false).build());

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
    CompletableFuture<EppoHttpResponse> configFetchFuture = new CompletableFuture<>();

    when(mockHttpClient.getAsync(anyString(), any())).thenReturn(configFetchFuture);

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
    initialConfigFuture.complete(new Configuration.Builder(flagConfig, false).build());

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
    when(mockHttpClient.get(anyString()))
        .thenReturn(new EppoHttpResponse(flagConfig.getBytes(), 200, null));
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
    when(mockHttpClient.get(anyString()))
        .thenReturn(new EppoHttpResponse("{}".getBytes(), 200, null));
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
    when(mockHttpClient.get(anyString()))
        .thenReturn(new EppoHttpResponse("{}".getBytes(), 200, null));
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
    when(mockHttpClient.getAsync(anyString(), any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new EppoHttpResponse("{\"flags\":{}}".getBytes(), 200, null)));
    when(mockConfigStore.getConfiguration()).thenReturn(Configuration.emptyConfig());

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
  public void testETagStoredFromSuccessfulFetch() throws IOException {
    // Setup
    ConfigurationStore store = new ConfigurationStore();
    EppoHttpClient mockClient = mock(EppoHttpClient.class);
    ConfigurationRequestor requestor = new ConfigurationRequestor(store, mockClient, false, false);

    String testETag = "test-etag-12345";
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    EppoHttpResponse response = new EppoHttpResponse(flagConfig.getBytes(), 200, testETag);

    when(mockClient.get(anyString())).thenReturn(response);

    // Execute
    requestor.fetchAndSaveFromRemote();

    // Verify eTag was stored
    Configuration config = store.getConfiguration();
    assertEquals(testETag, config.getFlagsETag());
  }

  @Test
  public void test304SkipsConfigurationUpdate() throws IOException {
    // Setup with existing config that has an eTag
    ConfigurationStore store = Mockito.spy(new ConfigurationStore());
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    Configuration existingConfig =
        Configuration.builder(flagConfig.getBytes()).flagsETag("old-etag").build();
    store.saveConfiguration(existingConfig).join();

    EppoHttpClient mockClient = mock(EppoHttpClient.class);
    ConfigurationRequestor requestor = new ConfigurationRequestor(store, mockClient, false, false);

    // Mock 304 response
    EppoHttpResponse response304 = new EppoHttpResponse(new byte[0], 304, "old-etag");
    when(mockClient.get(anyString())).thenReturn(response304);

    // Execute
    requestor.fetchAndSaveFromRemote();

    // Verify: Configuration NOT updated (saveConfiguration called only once - the initial setup)
    Mockito.verify(store, Mockito.times(1)).saveConfiguration(any());

    // Verify: Same config instance still in store
    assertEquals("old-etag", store.getConfiguration().getFlagsETag());
  }

  @Test
  public void test304DoesNotFireCallbacks() throws IOException {
    // Setup with existing config
    ConfigurationStore store = new ConfigurationStore();
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    Configuration existingConfig =
        Configuration.builder(flagConfig.getBytes()).flagsETag("test-etag").build();
    store.saveConfiguration(existingConfig).join();

    EppoHttpClient mockClient = mock(EppoHttpClient.class);
    ConfigurationRequestor requestor = new ConfigurationRequestor(store, mockClient, false, false);

    // Setup callback counter
    AtomicInteger callbackCount = new AtomicInteger(0);
    requestor.onConfigurationChange(config -> callbackCount.incrementAndGet());

    // Mock 304 response
    EppoHttpResponse response304 = new EppoHttpResponse(new byte[0], 304, "test-etag");
    when(mockClient.get(anyString())).thenReturn(response304);

    // Execute
    requestor.fetchAndSaveFromRemote();

    // Verify: Callback was NOT called
    assertEquals(0, callbackCount.get());
  }

  @Test
  public void testIfNoneMatchHeaderSentAsync() throws Exception {
    // Setup with existing config that has eTag
    ConfigurationStore store = new ConfigurationStore();
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    Configuration existingConfig =
        Configuration.builder(flagConfig.getBytes()).flagsETag("existing-etag").build();
    store.saveConfiguration(existingConfig).join();

    EppoHttpClient mockClient = mock(EppoHttpClient.class);
    ConfigurationRequestor requestor = new ConfigurationRequestor(store, mockClient, false, false);

    // Mock response
    EppoHttpResponse response = new EppoHttpResponse(flagConfig.getBytes(), 200, "new-etag");
    when(mockClient.getAsync(anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(response));

    // Execute async fetch
    requestor.fetchAndSaveFromRemoteAsync().join();

    // Verify: getAsync was called with the existing eTag
    Mockito.verify(mockClient).getAsync(anyString(), eq("existing-etag"));
  }

  @Test
  public void testNoIfNoneMatchHeaderWhenNoETag() throws Exception {
    // Setup with empty config (no eTag)
    ConfigurationStore store = new ConfigurationStore();

    EppoHttpClient mockClient = mock(EppoHttpClient.class);
    ConfigurationRequestor requestor = new ConfigurationRequestor(store, mockClient, false, false);

    // Mock response
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    EppoHttpResponse response = new EppoHttpResponse(flagConfig.getBytes(), 200, "new-etag");
    when(mockClient.getAsync(anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(response));

    // Execute
    requestor.fetchAndSaveFromRemoteAsync().join();

    // Verify: getAsync was called with null (no If-None-Match header)
    Mockito.verify(mockClient).getAsync(anyString(), isNull());
  }

  @Test
  public void testEmptyConfigHasNullETag() {
    Configuration emptyConfig = Configuration.emptyConfig();
    assertNull(emptyConfig.getFlagsETag());
  }

  @Test
  public void testETagRoundTripScenario() throws Exception {
    ConfigurationStore store = new ConfigurationStore();
    EppoHttpClient mockClient = mock(EppoHttpClient.class);
    ConfigurationRequestor requestor = new ConfigurationRequestor(store, mockClient, false, false);

    AtomicInteger callbackCount = new AtomicInteger(0);
    requestor.onConfigurationChange(config -> callbackCount.incrementAndGet());

    String flagConfig1 = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    String flagConfig2 =
        FileUtils.readFileToString(differentFlagConfigFile, StandardCharsets.UTF_8);

    // First fetch: 200 OK with eTag
    EppoHttpResponse response1 = new EppoHttpResponse(flagConfig1.getBytes(), 200, "etag-v1");
    when(mockClient.getAsync(anyString(), isNull()))
        .thenReturn(CompletableFuture.completedFuture(response1));

    requestor.fetchAndSaveFromRemoteAsync().join();

    assertEquals("etag-v1", store.getConfiguration().getFlagsETag());
    assertEquals(1, callbackCount.get());

    // Second fetch: 304 Not Modified
    EppoHttpResponse response2 = new EppoHttpResponse(new byte[0], 304, "etag-v1");
    when(mockClient.getAsync(anyString(), eq("etag-v1")))
        .thenReturn(CompletableFuture.completedFuture(response2));

    requestor.fetchAndSaveFromRemoteAsync().join();

    // ETag unchanged, callback not fired
    assertEquals("etag-v1", store.getConfiguration().getFlagsETag());
    assertEquals(1, callbackCount.get()); // Still 1, not 2

    // Third fetch: 200 OK with new eTag (data changed)
    EppoHttpResponse response3 = new EppoHttpResponse(flagConfig2.getBytes(), 200, "etag-v2");
    when(mockClient.getAsync(anyString(), eq("etag-v1")))
        .thenReturn(CompletableFuture.completedFuture(response3));

    requestor.fetchAndSaveFromRemoteAsync().join();

    // New eTag stored, callback fired
    assertEquals("etag-v2", store.getConfiguration().getFlagsETag());
    assertEquals(2, callbackCount.get()); // Now 2
  }

  @Test
  public void testBanditsNotFetchedOn304() throws IOException {
    // Setup with config that has bandits
    ConfigurationStore store = new ConfigurationStore();
    String flagConfigWithBandits =
        "{\"flags\":{},\"banditReferences\":{\"test-bandit\":{\"modelVersion\":\"v1\",\"flagVariations\":[]}}}";
    Configuration existingConfig =
        Configuration.builder(flagConfigWithBandits.getBytes()).flagsETag("test-etag").build();
    store.saveConfiguration(existingConfig).join();

    EppoHttpClient mockClient = mock(EppoHttpClient.class);
    ConfigurationRequestor requestor =
        new ConfigurationRequestor(store, mockClient, false, true); // bandits enabled

    // Mock 304 for flags
    EppoHttpResponse response304 = new EppoHttpResponse(new byte[0], 304, "test-etag");
    when(mockClient.get(Constants.FLAG_CONFIG_ENDPOINT)).thenReturn(response304);

    // Execute
    requestor.fetchAndSaveFromRemote();

    // Verify: Bandits endpoint was NOT called
    Mockito.verify(mockClient, Mockito.never()).get(Constants.BANDIT_ENDPOINT);
  }
}

package cloud.eppo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.eppo.api.Configuration;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class ConfigurationRequestorTest {
  private final File initialFlagConfigFile =
      new File("src/test/resources/static/initial-flag-config.json");
  private final File differentFlagConfigFile =
      new File("src/test/resources/static/boolean-flag.json");

  @Test
  public void testInitialConfigurationFuture() throws IOException {
    IConfigurationStore configStore = new ConfigurationStore();
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    ConfigurationRequestor requestor =
        new ConfigurationRequestor(configStore, mockHttpClient, false, true);

    CompletableFuture<Configuration> futureConfig = new CompletableFuture<>();
    byte[] flagConfig = FileUtils.readFileToByteArray(initialFlagConfigFile);

    requestor.setInitialConfiguration(futureConfig);

    assertNull(configStore.getConfiguration());

    futureConfig.complete(Configuration.builder(flagConfig, false).build());

    assertNotNull(configStore.getConfiguration());
    assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));
  }

  @Test
  public void testInitialConfigurationDoesntClobberFetch() throws IOException {
    IConfigurationStore configStore = new ConfigurationStore();
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    ConfigurationRequestor requestor =
        new ConfigurationRequestor(configStore, mockHttpClient, false, true);

    CompletableFuture<Configuration> initialConfigFuture = new CompletableFuture<>();
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    CompletableFuture<byte[]> configFetchFuture = new CompletableFuture<>();
    String fetchedFlagConfig =
        FileUtils.readFileToString(differentFlagConfigFile, StandardCharsets.UTF_8);

    when(mockHttpClient.getAsync("/api/flag-config/v1/config")).thenReturn(configFetchFuture);

    // Set initial config and verify that the config is still null as the inital config future has
    // not completed yet.
    requestor.setInitialConfiguration(initialConfigFuture);
    assertNull(configStore.getConfiguration());

    // The initial config contains only one flag keyed `numeric_flag`. The fetch response has only
    // one flag keyed
    // `boolean_flag`. We make sure to complete the fetch future first to verify the cache load does
    // not overwrite it.
    CompletableFuture<Void> handle = requestor.fetchAndSaveFromRemoteAsync();

    // Resolve the fetch and then the initialConfig
    configFetchFuture.complete(fetchedFlagConfig.getBytes(StandardCharsets.UTF_8));
    initialConfigFuture.complete(new Configuration.Builder(flagConfig, false).build());

    assertNotNull(configStore.getConfiguration());

    // `numeric_flag` is only in the cache which should have been ignored.
    assertNull(configStore.getConfiguration().getFlag("numeric_flag"));

    // `boolean_flag` is available only from the fetch
    assertNotNull(configStore.getConfiguration().getFlag("boolean_flag"));
  }

  @Test
  public void testBrokenFetchDoesntClobberCache() throws IOException {
    IConfigurationStore configStore = new ConfigurationStore();
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    ConfigurationRequestor requestor =
        new ConfigurationRequestor(configStore, mockHttpClient, false, true);

    CompletableFuture<Configuration> initialConfigFuture = new CompletableFuture<>();
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    CompletableFuture<byte[]> configFetchFuture = new CompletableFuture<>();

    when(mockHttpClient.getAsync("/api/flag-config/v1/config")).thenReturn(configFetchFuture);

    // Set initial config and verify that the config is still null as the inital config future has
    // not completed yet.
    requestor.setInitialConfiguration(initialConfigFuture);
    assertNull(configStore.getConfiguration());

    requestor.fetchAndSaveFromRemoteAsync();

    // Resolve the initial config
    initialConfigFuture.complete(new Configuration.Builder(flagConfig, false).build());

    // Error out the fetch
    configFetchFuture.completeExceptionally(new Exception("Intentional exception"));

    assertNotNull(configStore.getConfiguration());

    // `numeric_flag` is only in the cache which should be available
    assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));

    assertNull(configStore.getConfiguration().getFlag("boolean_flag"));
  }

  @Test
  public void testCacheWritesAfterBrokenFetch() throws IOException {
    IConfigurationStore configStore = new ConfigurationStore();
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    ConfigurationRequestor requestor =
        new ConfigurationRequestor(configStore, mockHttpClient, false, true);

    CompletableFuture<Configuration> initialConfigFuture = new CompletableFuture<>();
    String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, StandardCharsets.UTF_8);
    CompletableFuture<byte[]> configFetchFuture = new CompletableFuture<>();

    when(mockHttpClient.getAsync("/api/flag-config/v1/config")).thenReturn(configFetchFuture);

    // Set initial config and verify that the config is still null as the inital config future has
    // not completed yet.
    requestor.setInitialConfiguration(initialConfigFuture);
    assertNull(configStore.getConfiguration());

    requestor.fetchAndSaveFromRemoteAsync();

    // Error out the fetch
    configFetchFuture.completeExceptionally(new Exception("Intentional exception"));

    // Resolve the initial config after the fetch throws an error.
    initialConfigFuture.complete(new Configuration.Builder(flagConfig, false).build());

    assertNotNull(configStore.getConfiguration());

    // `numeric_flag` is only in the cache which should be available
    assertNotNull(configStore.getConfiguration().getFlag("numeric_flag"));

    assertNull(configStore.getConfiguration().getFlag("boolean_flag"));
  }
}

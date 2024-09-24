package cloud.eppo;

import cloud.eppo.api.Configuration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);
  private static final String FLAG_CONFIG_PATH = "/api/flag-config/v1/config";
  private static final String BANDIT_PARAMETER_PATH = "/api/flag-config/v1/bandits";

  private final EppoHttpClient client;
  private final IConfigurationStore configurationStore;
  private final boolean expectObfuscatedConfig;
  private final boolean supportBandits;

  public ConfigurationRequestor(
      @NotNull IConfigurationStore configurationStore,
      @NotNull EppoHttpClient client,
      boolean expectObfuscatedConfig,
      boolean supportBandits) {
    this.configurationStore = configurationStore;
    this.client = client;
    this.expectObfuscatedConfig = expectObfuscatedConfig;
    this.supportBandits = supportBandits;
  }

  public ConfigurationRequestor(
      @NotNull IConfigurationStore configurationStore,
      @NotNull EppoHttpClient client,
      boolean expectObfuscatedConfig) {
    this.configurationStore = configurationStore;
    this.client = client;
    this.expectObfuscatedConfig = expectObfuscatedConfig;
    this.supportBandits = true;
  }

  /** Loads configuration synchronously from the API server. */
  void fetchAndSaveFromRemote() {
    log.debug("Fetching configuration");

    // Reuse the `lastConfig` as its bandits may be useful
    Configuration lastConfig = configurationStore.getConfiguration();

    byte[] flagConfigurationJsonBytes = client.get(FLAG_CONFIG_PATH);
    Configuration.Builder configBuilder =
        new Configuration.Builder(flagConfigurationJsonBytes, expectObfuscatedConfig)
            .banditParametersFromConfig(lastConfig);

    if (supportBandits && configBuilder.requiresBanditModels()) {
      byte[] banditParametersJsonBytes = client.get(BANDIT_PARAMETER_PATH);
      configBuilder.banditParameters(banditParametersJsonBytes);
    }

    configurationStore.saveConfiguration(configBuilder.build());
  }

  /** Loads configuration asynchronously from the API server, off-thread. */
  CompletableFuture<Void> fetchAndSaveFromRemoteAsync() {
    log.debug("Fetching configuration from API server");
    final Configuration lastConfig = configurationStore.getConfiguration();

    return client
        .getAsync(FLAG_CONFIG_PATH)
        .thenApply(
            flagConfigJsonBytes -> {
              Configuration.Builder configBuilder =
                  new Configuration.Builder(flagConfigJsonBytes, expectObfuscatedConfig)
                      .banditParametersFromConfig(
                          lastConfig); // possibly reuse last bandit models loaded for efficiency.

              if (supportBandits && configBuilder.requiresBanditModels()) {
                byte[] banditParametersJsonBytes;
                try {
                  banditParametersJsonBytes = client.getAsync(BANDIT_PARAMETER_PATH).get();
                } catch (InterruptedException | ExecutionException e) {
                  log.error("Error fetching from remote: " + e.getMessage());
                  throw new RuntimeException(e);
                }
                if (banditParametersJsonBytes != null) {
                  configBuilder.banditParameters(banditParametersJsonBytes);
                }
              }
              return configBuilder.build();
            })
        .thenApply(
            configuration -> {
              configurationStore.saveConfiguration(configuration);
              return null;
            });
  }
}

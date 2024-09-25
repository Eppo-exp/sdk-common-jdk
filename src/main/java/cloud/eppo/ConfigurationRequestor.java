package cloud.eppo;

import cloud.eppo.api.Configuration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

  private CompletableFuture<Void> remoteFetchFuture = null;
  private CompletableFuture<Void> configurationFuture = null;
  private boolean initialConfigSet = false;

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

  // Synchronously set the initial configuration.
  public void setInitialConfiguration(@NotNull Configuration configuration) {
    if (initialConfigSet || this.configurationFuture != null) {
      throw new IllegalStateException("Initial configuration has already been set");
    }

    try {
      configurationStore.saveConfiguration(configuration).join();
      initialConfigSet = true;
    } catch (CompletionException e) {
      log.error("Error setting initial configuration", e);
    }
  }

  // Asynchronously sets the initial configuration.
  public CompletableFuture<Void> setInitialConfiguration(
      @NotNull CompletableFuture<Configuration> configurationFuture) {
    if (initialConfigSet || this.configurationFuture != null) {
      throw new IllegalStateException("Configuration future has already been set");
    }
    this.configurationFuture =
        configurationFuture.thenAccept(
            (config) -> {
              synchronized (configurationStore) {
                if (config == null) {
                  log.debug("Initial configuration future returned null");
                } else if (remoteFetchFuture != null
                    && remoteFetchFuture.isDone()
                    && !remoteFetchFuture.isCompletedExceptionally()) {
                  // Don't clobber a successful fetch.
                  log.debug("Fetch successfully beat the initial config; not clobbering");
                } else {
                  log.debug("saving initial configuration");
                  configurationStore.saveConfiguration(config);
                  initialConfigSet = true;
                }
              }
            });
    return this.configurationFuture;
  }

  /** Loads configuration synchronously from the API server. */
  void fetchAndSaveFromRemote() {
    log.debug("Fetching configuration");

    // Reuse the `lastConfig` as its bandits may be useful
    Configuration lastConfig = configurationStore.getConfiguration();

    byte[] flagConfigurationJsonBytes = client.get(FLAG_CONFIG_PATH);
    Configuration.Builder configBuilder =
        Configuration.builder(flagConfigurationJsonBytes, expectObfuscatedConfig)
            .banditParametersFromConfig(lastConfig);

    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
      byte[] banditParametersJsonBytes = client.get(BANDIT_PARAMETER_PATH);
      configBuilder.banditParameters(banditParametersJsonBytes);
    }

    configurationStore.saveConfiguration(configBuilder.build());
  }

  /** Loads configuration asynchronously from the API server, off-thread. */
  CompletableFuture<Void> fetchAndSaveFromRemoteAsync() {
    log.debug("Fetching configuration from API server");
    final Configuration lastConfig = configurationStore.getConfiguration();

    if (remoteFetchFuture != null && !remoteFetchFuture.isDone()) {
      log.debug("Remote fetch is active. Cancelling and restarting");
      remoteFetchFuture.cancel(true);
      remoteFetchFuture = null;
    }

    remoteFetchFuture =
        client
            .getAsync(FLAG_CONFIG_PATH)
            .thenApply(
                flagConfigJsonBytes -> {
                  synchronized (this) {
                    Configuration.Builder configBuilder =
                        Configuration.builder(flagConfigJsonBytes, expectObfuscatedConfig)
                            .banditParametersFromConfig(
                                lastConfig); // possibly reuse last bandit models loaded.

                    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
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
                  }
                })
            .thenApply(
                configuration -> {
                  synchronized (configurationStore) {
                    configurationStore.saveConfiguration(configuration);
                  }
                  return null;
                });
    return remoteFetchFuture;
  }
}

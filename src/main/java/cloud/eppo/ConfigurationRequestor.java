package cloud.eppo;

import cloud.eppo.api.Configuration;
import cloud.eppo.callback.CallbackManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final EppoHttpClient client;
  private final IConfigurationStore configurationStore;
  private final boolean expectObfuscatedConfig;
  private final boolean supportBandits;

  private CompletableFuture<Void> remoteFetchFuture = null;
  private CompletableFuture<Boolean> configurationFuture = null;
  private boolean initialConfigSet = false;

  private final CallbackManager<Configuration> configChangeManager = new CallbackManager<>();

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

    initialConfigSet = saveConfigurationAndNotify(configuration).thenApply(v -> true).join();
  }

  /**
   * Asynchronously sets the initial configuration. Resolves to `true` if the initial configuration
   * was used, false if not (due to being empty, a fetched config taking precedence, etc.)
   */
  public CompletableFuture<Boolean> setInitialConfiguration(
      @NotNull CompletableFuture<Configuration> configurationFuture) {
    if (initialConfigSet || this.configurationFuture != null) {
      throw new IllegalStateException("Configuration future has already been set");
    }
    this.configurationFuture =
        configurationFuture
            .thenApply(
                (config) -> {
                  synchronized (configurationStore) {
                    if (config == null || config.isEmpty()) {
                      log.debug("Initial configuration future returned empty/null");
                      return false;
                    } else if (remoteFetchFuture != null
                        && remoteFetchFuture.isDone()
                        && !remoteFetchFuture.isCompletedExceptionally()) {
                      // Don't clobber a successful fetch.
                      log.debug("Fetch has completed; ignoring initial config load.");
                      return false;
                    } else {
                      initialConfigSet =
                          saveConfigurationAndNotify(config).thenApply((s) -> true).join();
                      return true;
                    }
                  }
                })
            .exceptionally(
                (e) -> {
                  log.error("Error setting initial config", e);
                  return false;
                });
    return this.configurationFuture;
  }

  /** Loads configuration synchronously from the API server. */
  void fetchAndSaveFromRemote() {
    log.debug("Fetching configuration");

    // Reuse the `lastConfig` as its bandits may be useful
    Configuration lastConfig = configurationStore.getConfiguration();

    byte[] flagConfigurationJsonBytes = client.get(Constants.FLAG_CONFIG_ENDPOINT);
    Configuration.Builder configBuilder =
        Configuration.builder(flagConfigurationJsonBytes, expectObfuscatedConfig)
            .banditParametersFromConfig(lastConfig);

    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
      byte[] banditParametersJsonBytes = client.get(Constants.BANDIT_ENDPOINT);
      configBuilder.banditParameters(banditParametersJsonBytes);
    }

    saveConfigurationAndNotify(configBuilder.build()).join();
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
            .getAsync(Constants.FLAG_CONFIG_ENDPOINT)
            .thenCompose(
                flagConfigJsonBytes -> {
                  synchronized (this) {
                    Configuration.Builder configBuilder =
                        Configuration.builder(flagConfigJsonBytes, expectObfuscatedConfig)
                            .banditParametersFromConfig(
                                lastConfig); // possibly reuse last bandit models loaded.

                    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
                      byte[] banditParametersJsonBytes;
                      try {
                        banditParametersJsonBytes =
                            client.getAsync(Constants.BANDIT_ENDPOINT).get();
                      } catch (InterruptedException | ExecutionException e) {
                        log.error("Error fetching from remote: " + e.getMessage());
                        throw new RuntimeException(e);
                      }
                      if (banditParametersJsonBytes != null) {
                        configBuilder.banditParameters(banditParametersJsonBytes);
                      }
                    }

                    return saveConfigurationAndNotify(configBuilder.build());
                  }
                });
    return remoteFetchFuture;
  }

  private CompletableFuture<Void> saveConfigurationAndNotify(Configuration configuration) {
    CompletableFuture<Void> saveFuture = configurationStore.saveConfiguration(configuration);
    return saveFuture.thenRun(
        () -> {
          synchronized (configChangeManager) {
            configChangeManager.notifyCallbacks(configuration);
          }
        });
  }

  public Runnable onConfigurationChange(Consumer<Configuration> callback) {
    return configChangeManager.subscribe(callback);
  }
}

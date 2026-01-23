package cloud.eppo;

import cloud.eppo.api.Configuration;
import cloud.eppo.api.IBanditParametersResponse;
import cloud.eppo.api.IFlagConfigResponse;
import cloud.eppo.api.configuration.ConfigurationRequest;
import cloud.eppo.api.configuration.ConfigurationResponse;
import cloud.eppo.api.configuration.IEppoConfigurationHttpClient;
import cloud.eppo.callback.CallbackManager;
import cloud.eppo.configuration.ConfigurationRequestFactory;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final IEppoConfigurationHttpClient configurationClient;
  private final ConfigurationRequestFactory requestFactory;
  private final IConfigurationStore configurationStore;
  private final boolean supportBandits;

  private CompletableFuture<Void> remoteFetchFuture = null;
  private CompletableFuture<Boolean> configurationFuture = null;
  private boolean initialConfigSet = false;

  private final CallbackManager<Configuration> configChangeManager = new CallbackManager<>();

  public ConfigurationRequestor(
      @NotNull IConfigurationStore configurationStore,
      @NotNull IEppoConfigurationHttpClient configurationClient,
      @NotNull ConfigurationRequestFactory requestFactory,
      boolean supportBandits) {
    this.configurationStore = configurationStore;
    this.configurationClient = configurationClient;
    this.requestFactory = requestFactory;
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

    Configuration lastConfig = configurationStore.getConfiguration();
    String previousETag = extractETagFromConfig(lastConfig);

    ConfigurationRequest flagRequest = requestFactory.createFlagConfigurationRequest(previousETag);

    ConfigurationResponse<IFlagConfigResponse> flagResponse;
    try {
      flagResponse = configurationClient.fetchFlagConfiguration(flagRequest).get();
    } catch (Exception e) {
      log.error("Failed to fetch flag configuration", e);
      throw new RuntimeException("Failed to fetch flag configuration", e);
    }

    if (flagResponse.isError()) {
      throw new RuntimeException(
          "Failed to fetch flag configuration: " + flagResponse.getErrorMessage());
    }

    if (flagResponse.isNotModified()) {
      log.debug("Configuration not modified");
      return;
    }

    Configuration.Builder configBuilder =
        Configuration.builder(flagResponse.getPayload(), flagResponse.getETag())
            .banditParametersFromConfig(lastConfig);

    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
      ConfigurationRequest banditRequest = requestFactory.createBanditConfigurationRequest(null);
      try {
        ConfigurationResponse<IBanditParametersResponse> banditResponse =
            configurationClient.fetchBanditConfiguration(banditRequest).get();
        if (banditResponse.isSuccess()) {
          configBuilder.banditParameters(banditResponse.getPayload());
        }
      } catch (Exception e) {
        log.warn("Failed to fetch bandit configuration, continuing without bandits", e);
      }
    }

    saveConfigurationAndNotify(configBuilder.build()).join();
  }

  /** Loads configuration asynchronously from the API server, off-thread. */
  CompletableFuture<Void> fetchAndSaveFromRemoteAsync() {
    log.debug("Fetching configuration from API server");
    final Configuration lastConfig = configurationStore.getConfiguration();
    String previousETag = extractETagFromConfig(lastConfig);

    if (remoteFetchFuture != null && !remoteFetchFuture.isDone()) {
      log.debug("Remote fetch is active. Cancelling and restarting");
      remoteFetchFuture.cancel(true);
      remoteFetchFuture = null;
    }

    ConfigurationRequest flagRequest = requestFactory.createFlagConfigurationRequest(previousETag);

    remoteFetchFuture =
        configurationClient
            .fetchFlagConfiguration(flagRequest)
            .thenCompose(
                flagResponse -> {
                  if (flagResponse.isError()) {
                    throw new RuntimeException(
                        "Failed to fetch flag configuration: " + flagResponse.getErrorMessage());
                  }

                  if (flagResponse.isNotModified()) {
                    log.debug("Configuration not modified");
                    return CompletableFuture.completedFuture(null);
                  }

                  synchronized (this) {
                    Configuration.Builder configBuilder =
                        Configuration.builder(flagResponse.getPayload(), flagResponse.getETag())
                            .banditParametersFromConfig(lastConfig);

                    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
                      ConfigurationRequest banditRequest =
                          requestFactory.createBanditConfigurationRequest(null);
                      return configurationClient
                          .fetchBanditConfiguration(banditRequest)
                          .thenCompose(
                              banditResponse -> {
                                if (banditResponse.isSuccess()) {
                                  configBuilder.banditParameters(banditResponse.getPayload());
                                }
                                return saveConfigurationAndNotify(configBuilder.build());
                              });
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

  /**
   * Unsubscribe from configuration change notifications.
   *
   * @param callback The callback to unsubscribe
   * @return true if the callback was found and removed, false otherwise
   */
  public boolean unsubscribeFromConfigurationChange(Consumer<Configuration> callback) {
    return configChangeManager.unsubscribe(callback);
  }

  private String extractETagFromConfig(Configuration config) {
    if (config == null) {
      return null;
    }
    return config.getFlagsSnapshotId();
  }
}

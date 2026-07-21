package cloud.eppo;

import cloud.eppo.api.SerializableEppoConfiguration;
import cloud.eppo.callback.CallbackManager;
import cloud.eppo.http.EppoConfigurationClient;
import cloud.eppo.http.EppoConfigurationRequest;
import cloud.eppo.http.EppoConfigurationRequestFactory;
import cloud.eppo.http.EppoConfigurationResponse;
import cloud.eppo.parser.ConfigurationParser;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRequestor<
    ConfigurationType extends SerializableEppoConfiguration, JsonFlagType> {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final IConfigurationStore<ConfigurationType> configurationStore;
  private final boolean supportBandits;

  @NotNull private final ConfigurationParser<ConfigurationType, JsonFlagType> configurationParser;

  @NotNull private final EppoConfigurationClient configurationClient;
  @NotNull private final EppoConfigurationRequestFactory requestFactory;

  private volatile CompletableFuture<Void> remoteFetchFuture = null;
  private volatile CompletableFuture<Boolean> configurationFuture = null;
  private volatile boolean initialConfigSet = false;

  private final CallbackManager<ConfigurationType> configChangeManager = new CallbackManager<>();

  public ConfigurationRequestor(
      @NotNull IConfigurationStore<ConfigurationType> configurationStore,
      boolean supportBandits,
      @NotNull ConfigurationParser<ConfigurationType, JsonFlagType> configurationParser,
      @NotNull EppoConfigurationClient configurationClient,
      @NotNull EppoConfigurationRequestFactory requestFactory) {
    this.configurationStore = configurationStore;
    this.supportBandits = supportBandits;
    this.configurationParser = configurationParser;
    this.configurationClient = configurationClient;
    this.requestFactory = requestFactory;
  }

  // Synchronously set the initial configuration.
  public void setInitialConfiguration(@NotNull ConfigurationType configuration) {
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
      @NotNull CompletableFuture<ConfigurationType> configurationFuture) {
    if (initialConfigSet || this.configurationFuture != null) {
      throw new IllegalStateException("Configuration future has already been set");
    }
    this.configurationFuture =
        configurationFuture
            .thenCompose(
                (config) -> {
                  synchronized (configurationStore) {
                    if (config == null || config.isEmpty()) {
                      log.debug("Initial configuration future returned empty/null");
                      return CompletableFuture.completedFuture(false);
                    } else if (remoteFetchFuture != null
                        && remoteFetchFuture.isDone()
                        && !remoteFetchFuture.isCompletedExceptionally()) {
                      // Don't clobber a successful fetch.
                      log.debug("Fetch has completed; ignoring initial config load.");
                      return CompletableFuture.completedFuture(false);
                    } else {
                      return saveConfigurationAndNotify(config)
                          .thenApply(
                              (s) -> {
                                initialConfigSet = true;
                                return true;
                              });
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
    ConfigurationType lastConfig = configurationStore.getConfiguration();

    EppoConfigurationRequest flagRequest =
        requestFactory.createFlagConfigRequest(lastConfig.getFlagsSnapshotId());
    EppoConfigurationResponse flagResponse;
    try {
      flagResponse = configurationClient.execute(flagRequest).get();
    } catch (InterruptedException e) {
      log.error("Config fetch interrupted", e);
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      log.error("Config fetch interrupted", e);
      throw new RuntimeException(e);
    }

    if (flagResponse.isNotModified()) {
      log.debug("Flag configuration not modified");
      return;
    }

    if (!flagResponse.isSuccessful()) {
      throw new RuntimeException(
          "Failed to fetch flag configuration. Status: " + flagResponse.getStatusCode());
    }

    // Phase 1: build config from flags, carrying over previous bandits
    ConfigurationType config =
        configurationParser.buildConfig(
            flagResponse.getBody(), flagResponse.getVersionId(), lastConfig);

    // Phase 2: fetch and apply fresh bandits if needed
    if (supportBandits && configurationParser.requiresUpdatedBanditModels(config)) {
      byte[] banditBytes = fetchBanditParameterBytes();
      if (banditBytes != null) {
        config = configurationParser.applyBanditParameters(config, banditBytes);
      }
    }

    saveConfigurationAndNotify(config).join();
  }

  /** Fetches bandit parameters from the configuration client. */
  private byte[] fetchBanditParameterBytes() {
    EppoConfigurationRequest banditRequest = requestFactory.createBanditParamsRequest();
    try {
      EppoConfigurationResponse banditResponse = configurationClient.execute(banditRequest).get();
      if (banditResponse.isSuccessful() && banditResponse.getBody() != null) {
        return banditResponse.getBody();
      }
      return null;
    } catch (InterruptedException e) {
      log.error("Error fetching bandit parameters", e);
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      log.error("Error fetching bandit parameters", e);
      throw new RuntimeException(e);
    }
  }

  /** Loads configuration asynchronously from the API server, off-thread. */
  CompletableFuture<Void> fetchAndSaveFromRemoteAsync() {
    log.debug("Fetching configuration from API server");
    final ConfigurationType lastConfig = configurationStore.getConfiguration();

    if (remoteFetchFuture != null && !remoteFetchFuture.isDone()) {
      log.debug("Remote fetch is active. Cancelling and restarting");
      remoteFetchFuture.cancel(true);
      remoteFetchFuture = null;
    }

    EppoConfigurationRequest flagRequest =
        requestFactory.createFlagConfigRequest(lastConfig.getFlagsSnapshotId());

    remoteFetchFuture =
        configurationClient
            .execute(flagRequest)
            .thenCompose(
                flagResponse -> {
                  synchronized (this) {
                    if (flagResponse.isNotModified()) {
                      log.debug("Flag configuration not modified");
                      return CompletableFuture.completedFuture(null);
                    }

                    if (!flagResponse.isSuccessful()) {
                      throw new RuntimeException(
                          "Failed to fetch flag configuration. Status: "
                              + flagResponse.getStatusCode());
                    }

                    return buildAndSaveConfiguration(flagResponse, lastConfig);
                  }
                });

    return remoteFetchFuture;
  }

  // Common handling for building config and conditionally loading bandit parameters, async.
  private CompletableFuture<Void> buildAndSaveConfiguration(
      EppoConfigurationResponse flagResponse, ConfigurationType lastConfig) {

    // Phase 1: build config from flags, carrying over previous bandits
    ConfigurationType config =
        configurationParser.buildConfig(
            flagResponse.getBody(), flagResponse.getVersionId(), lastConfig);

    // Phase 2: fetch and apply fresh bandits if needed
    if (supportBandits && configurationParser.requiresUpdatedBanditModels(config)) {
      byte[] banditBytes = fetchBanditParameterBytes();
      if (banditBytes != null) {
        config = configurationParser.applyBanditParameters(config, banditBytes);
      }
    }

    return saveConfigurationAndNotify(config);
  }

  private CompletableFuture<Void> saveConfigurationAndNotify(ConfigurationType configuration) {
    CompletableFuture<Void> saveFuture = configurationStore.saveConfiguration(configuration);
    return saveFuture.thenRun(
        () -> {
          synchronized (configChangeManager) {
            configChangeManager.notifyCallbacks(configuration);
          }
        });
  }

  public Runnable onConfigurationChange(Consumer<ConfigurationType> callback) {
    return configChangeManager.subscribe(callback);
  }

  /**
   * Unsubscribe from configuration change notifications.
   *
   * @param callback The callback to unsubscribe
   * @return true if the callback was found and removed, false otherwise
   */
  public boolean unsubscribeFromConfigurationChange(Consumer<ConfigurationType> callback) {
    return configChangeManager.unsubscribe(callback);
  }
}

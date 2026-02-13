package cloud.eppo;

import cloud.eppo.api.Configuration;
import cloud.eppo.api.dto.BanditParametersResponse;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.callback.CallbackManager;
import cloud.eppo.http.EppoConfigurationClient;
import cloud.eppo.http.EppoConfigurationRequest;
import cloud.eppo.http.EppoConfigurationRequestFactory;
import cloud.eppo.http.EppoConfigurationResponse;
import cloud.eppo.parser.ConfigurationParseException;
import cloud.eppo.parser.ConfigurationParser;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final IConfigurationStore configurationStore;
  private final boolean supportBandits;

  @NotNull private final ConfigurationParser configurationParser;
  @NotNull private final EppoConfigurationClient configurationClient;
  @NotNull private final EppoConfigurationRequestFactory requestFactory;

  private CompletableFuture<Void> remoteFetchFuture = null;
  private CompletableFuture<Boolean> configurationFuture = null;
  private boolean initialConfigSet = false;

  private final CallbackManager<Configuration> configChangeManager = new CallbackManager<>();

  public ConfigurationRequestor(
      @NotNull IConfigurationStore configurationStore,
      boolean supportBandits,
      @NotNull ConfigurationParser configurationParser,
      @NotNull EppoConfigurationClient configurationClient,
      @NotNull EppoConfigurationRequestFactory requestFactory) {
    this.configurationStore = configurationStore;
    this.supportBandits = supportBandits;
    this.configurationParser = configurationParser;
    this.configurationClient = configurationClient;
    this.requestFactory = requestFactory;
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

    EppoConfigurationRequest flagRequest =
        requestFactory.createFlagConfigRequest(lastConfig.getFlagsSnapshotId());
    EppoConfigurationResponse flagResponse;
    try {
      flagResponse = configurationClient.get(flagRequest).get();
    } catch (InterruptedException | ExecutionException e) {
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

    byte[] flagConfigurationJsonBytes = flagResponse.getBody();

    Configuration.Builder configBuilder;
    try {
      FlagConfigResponse flagConfigResponse =
          configurationParser.parseFlagConfig(flagConfigurationJsonBytes);
      configBuilder =
          new Configuration.Builder(flagConfigResponse).banditParametersFromConfig(lastConfig);
    } catch (ConfigurationParseException e) {
      log.error("Failed to parse flag configuration", e);
      throw new RuntimeException(e);
    }

    configBuilder.flagsSnapshotId(flagResponse.getVersionId());

    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
      byte[] banditParametersJsonBytes = fetchBanditParameters();
      if (banditParametersJsonBytes != null) {
        try {
          BanditParametersResponse bandits =
              configurationParser.parseBanditParams(banditParametersJsonBytes);
          configBuilder.banditParameters(bandits);
        } catch (ConfigurationParseException e) {
          log.error("Failed to parse bandit parameters", e);
          throw new RuntimeException(e);
        }
      }
    }

    saveConfigurationAndNotify(configBuilder.build()).join();
  }

  /** Fetches bandit parameters from the configuration client. */
  private byte[] fetchBanditParameters() {
    EppoConfigurationRequest banditRequest = requestFactory.createBanditParamsRequest();
    EppoConfigurationResponse banditResponse;
    try {
      banditResponse = configurationClient.get(banditRequest).get();
    } catch (InterruptedException | ExecutionException e) {
      log.error("Bandit fetch interrupted", e);
      throw new RuntimeException(e);
    }

    if (banditResponse.isSuccessful() && banditResponse.getBody() != null) {
      return banditResponse.getBody();
    }
    return null;
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

    EppoConfigurationRequest flagRequest =
        requestFactory.createFlagConfigRequest(lastConfig.getFlagsSnapshotId());

    remoteFetchFuture =
        configurationClient
            .get(flagRequest)
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
      EppoConfigurationResponse flagResponse, Configuration lastConfig) {
    Configuration.Builder configBuilder;

    try {
      FlagConfigResponse flagConfigResponse =
          configurationParser.parseFlagConfig(flagResponse.getBody());
      configBuilder =
          new Configuration.Builder(flagConfigResponse).banditParametersFromConfig(lastConfig);
    } catch (ConfigurationParseException e) {
      log.error("Failed to parse flag configuration", e);
      throw new RuntimeException(e);
    }

    configBuilder.flagsSnapshotId(flagResponse.getVersionId());

    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
      byte[] banditParametersJsonBytes = fetchBanditParametersAsync();
      if (banditParametersJsonBytes != null) {
        try {
          BanditParametersResponse bandits =
              configurationParser.parseBanditParams(banditParametersJsonBytes);
          configBuilder.banditParameters(bandits);
        } catch (ConfigurationParseException e) {
          log.error("Failed to parse bandit parameters", e);
          throw new RuntimeException(e);
        }
      }
    }

    return saveConfigurationAndNotify(configBuilder.build());
  }

  /** Fetches bandit parameters synchronously (used within async flow). */
  private byte[] fetchBanditParametersAsync() {
    EppoConfigurationRequest banditRequest = requestFactory.createBanditParamsRequest();
    try {
      EppoConfigurationResponse banditResponse = configurationClient.get(banditRequest).get();
      if (banditResponse.isSuccessful() && banditResponse.getBody() != null) {
        return banditResponse.getBody();
      }
      return null;
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error fetching bandit parameters: " + e.getMessage());
      throw new RuntimeException(e);
    }
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
}

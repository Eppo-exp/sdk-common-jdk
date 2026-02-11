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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final EppoHttpClient client;
  private final IConfigurationStore configurationStore;
  private final boolean supportBandits;

  // Optional custom implementations
  @Nullable private final ConfigurationParser configurationParser;
  @Nullable private final EppoConfigurationClient eppoConfigurationClient;
  @NotNull private final EppoConfigurationRequestFactory requestFactory;

  private CompletableFuture<Void> remoteFetchFuture = null;
  private CompletableFuture<Boolean> configurationFuture = null;
  private boolean initialConfigSet = false;

  private final CallbackManager<Configuration> configChangeManager = new CallbackManager<>();

  public ConfigurationRequestor(
      @NotNull IConfigurationStore configurationStore,
      @NotNull EppoHttpClient client,
      boolean expectObfuscatedConfig,
      boolean supportBandits,
      @Nullable ConfigurationParser configurationParser,
      @Nullable EppoConfigurationClient eppoConfigurationClient,
      @NotNull EppoConfigurationRequestFactory requestFactory) {
    this.configurationStore = configurationStore;
    this.client = client;
    this.supportBandits = supportBandits;
    this.configurationParser = configurationParser;
    this.eppoConfigurationClient = eppoConfigurationClient;
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

    byte[] flagConfigurationJsonBytes;
    FlagConfigResponse flagConfigResponse = null;

    String flagsSnapshotId = null;
    // Use EppoConfigurationClient if available, otherwise fall back to EppoHttpClient
    if (eppoConfigurationClient != null) {
      EppoConfigurationRequest flagRequest =
          requestFactory.createFlagConfigRequest(lastConfig.getFlagsSnapshotId());
      EppoConfigurationResponse flagResponse;
      try {
        flagResponse = eppoConfigurationClient.get(flagRequest).get();
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

      flagConfigurationJsonBytes = flagResponse.getBody();
      flagsSnapshotId = flagResponse.getVersionId();
    } else {
      flagConfigurationJsonBytes = client.get(Constants.FLAG_CONFIG_ENDPOINT);
    }

    // Use ConfigurationParser if available, otherwise use Configuration.builder
    Configuration.Builder configBuilder;
    if (configurationParser != null) {
      try {
        flagConfigResponse = configurationParser.parseFlagConfig(flagConfigurationJsonBytes);
        configBuilder =
            new Configuration.Builder(flagConfigurationJsonBytes, flagConfigResponse)
                .banditParametersFromConfig(lastConfig);
      } catch (ConfigurationParseException e) {
        log.error("Failed to parse flag configuration", e);
        throw new RuntimeException(e);
      }
    } else {
      configBuilder =
          Configuration.builder(flagConfigurationJsonBytes).banditParametersFromConfig(lastConfig);
    }

    configBuilder.flagsSnapshotId(flagsSnapshotId);

    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
      byte[] banditParametersJsonBytes = fetchBanditParameters();
      if (banditParametersJsonBytes != null) {
        if (configurationParser != null) {
          try {
            BanditParametersResponse bandits =
                configurationParser.parseBanditParams(banditParametersJsonBytes);
            configBuilder.banditParameters(bandits);
          } catch (ConfigurationParseException e) {
            log.error("Failed to parse bandit parameters", e);
            throw new RuntimeException(e);
          }
        } else {
          configBuilder.banditParameters(banditParametersJsonBytes);
        }
      }
    }

    saveConfigurationAndNotify(configBuilder.build()).join();
  }

  /** Fetches bandit parameters using the appropriate client. */
  private byte[] fetchBanditParameters() {
    if (eppoConfigurationClient != null) {
      EppoConfigurationRequest banditRequest = requestFactory.createBanditParamsRequest();
      EppoConfigurationResponse banditResponse;
      try {
        banditResponse = eppoConfigurationClient.get(banditRequest).get();
      } catch (InterruptedException | ExecutionException e) {
        log.error("Bandit fetch interrupted", e);
        throw new RuntimeException(e);
      }

      if (banditResponse.isSuccessful() && banditResponse.getBody() != null) {
        return banditResponse.getBody();
      }
      return null;
    } else {
      return client.get(Constants.BANDIT_ENDPOINT);
    }
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

    // Use EppoConfigurationClient if available, otherwise fall back to EppoHttpClient
    if (eppoConfigurationClient != null) {
      EppoConfigurationRequest flagRequest =
          requestFactory.createFlagConfigRequest(lastConfig.getFlagsSnapshotId());

      remoteFetchFuture =
          eppoConfigurationClient
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

                      byte[] flagConfigJsonBytes = flagResponse.getBody();

                      return buildAndSaveConfiguration(flagConfigJsonBytes, lastConfig);
                    }
                  });
    } else {
      remoteFetchFuture =
          client
              .getAsync(Constants.FLAG_CONFIG_ENDPOINT)
              .thenCompose(
                  flagConfigJsonBytes -> {
                    synchronized (this) {
                      return buildAndSaveConfiguration(flagConfigJsonBytes, lastConfig);
                    }
                  });
    }

    return remoteFetchFuture;
  }

  /** Builds configuration from flag bytes and saves it. Used by async fetch. */
  private CompletableFuture<Void> buildAndSaveConfiguration(
      byte[] flagConfigJsonBytes, Configuration lastConfig) {
    Configuration.Builder configBuilder;

    // Use ConfigurationParser if available
    if (configurationParser != null) {
      try {
        FlagConfigResponse flagConfigResponse =
            configurationParser.parseFlagConfig(flagConfigJsonBytes);
        configBuilder =
            new Configuration.Builder(flagConfigJsonBytes, flagConfigResponse)
                .banditParametersFromConfig(lastConfig);
      } catch (ConfigurationParseException e) {
        log.error("Failed to parse flag configuration", e);
        throw new RuntimeException(e);
      }
    } else {
      configBuilder =
          Configuration.builder(flagConfigJsonBytes).banditParametersFromConfig(lastConfig);
    }

    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
      byte[] banditParametersJsonBytes = fetchBanditParametersAsync();
      if (banditParametersJsonBytes != null) {
        if (configurationParser != null) {
          try {
            BanditParametersResponse bandits =
                configurationParser.parseBanditParams(banditParametersJsonBytes);
            configBuilder.banditParameters(bandits);
          } catch (ConfigurationParseException e) {
            log.error("Failed to parse bandit parameters", e);
            throw new RuntimeException(e);
          }
        } else {
          configBuilder.banditParameters(banditParametersJsonBytes);
        }
      }
    }

    return saveConfigurationAndNotify(configBuilder.build());
  }

  /** Fetches bandit parameters synchronously (used within async flow). */
  private byte[] fetchBanditParametersAsync() {
    if (eppoConfigurationClient != null) {
      EppoConfigurationRequest banditRequest = requestFactory.createBanditParamsRequest();
      try {
        EppoConfigurationResponse banditResponse = eppoConfigurationClient.get(banditRequest).get();
        if (banditResponse.isSuccessful() && banditResponse.getBody() != null) {
          return banditResponse.getBody();
        }
        return null;
      } catch (InterruptedException | ExecutionException e) {
        log.error("Error fetching bandit parameters: " + e.getMessage());
        throw new RuntimeException(e);
      }
    } else {
      try {
        return client.getAsync(Constants.BANDIT_ENDPOINT).get();
      } catch (InterruptedException | ExecutionException e) {
        log.error("Error fetching from remote: " + e.getMessage());
        throw new RuntimeException(e);
      }
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

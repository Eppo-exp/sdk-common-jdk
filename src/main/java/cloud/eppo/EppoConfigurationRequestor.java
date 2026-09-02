package cloud.eppo;

import cloud.eppo.api.SerializableEppoConfiguration;
import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.http.EppoConfigurationClient;
import cloud.eppo.http.EppoConfigurationRequest;
import cloud.eppo.http.EppoConfigurationRequestFactory;
import cloud.eppo.http.EppoConfigurationResponse;
import cloud.eppo.parser.ConfigurationParseException;
import cloud.eppo.parser.ConfigurationParser;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EppoConfigurationRequestor<
        ConfigurationType extends SerializableEppoConfiguration, JsonFlagType>
    implements ConfigurationRequestor<ConfigurationType> {
  private static final Logger log = LoggerFactory.getLogger(EppoConfigurationRequestor.class);

  private final IConfigurationStore<ConfigurationType> configurationStore;
  private final boolean supportBandits;

  @NotNull private final ConfigurationParser<ConfigurationType, JsonFlagType> configurationParser;

  @NotNull private final EppoConfigurationClient configurationClient;
  @NotNull private final EppoConfigurationRequestFactory requestFactory;

  private volatile CompletableFuture<Void> remoteFetchFuture = null;
  private volatile CompletableFuture<Boolean> configurationFuture = null;
  private volatile boolean initialConfigSet = false;

  public EppoConfigurationRequestor(
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

  /**
   * Asynchronously sets the initial configuration. Resolves to {@code true} if the initial
   * configuration was used, false if not (due to being empty, a fetched config taking precedence,
   * etc.)
   */
  @Override
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
                      return configurationStore
                          .saveConfiguration(config)
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
  @Override
  public void fetchAndSaveFromRemote() {
    log.debug("Fetching configuration");

    // Reuse the lastConfig as its bandits may be useful
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
      log.error("Config fetch failed", e);
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

    byte[] flagBody = flagResponse.getBody();
    if (flagBody == null) {
      throw new RuntimeException("Flag configuration response body is null");
    }
    FlagConfigResponse flagConfigResponse;
    try {
      flagConfigResponse = configurationParser.parseFlagConfig(flagBody);
    } catch (ConfigurationParseException e) {
      log.error("Failed to parse flag configuration", e);
      throw new RuntimeException(e);
    }

    byte[] banditBytes = null;
    if (needsFreshBandits(flagConfigResponse, lastConfig)) {
      EppoConfigurationRequest banditRequest = requestFactory.createBanditParamsRequest();
      try {
        EppoConfigurationResponse banditResponse = configurationClient.execute(banditRequest).get();
        if (banditResponse.isSuccessful() && banditResponse.getBody() != null) {
          banditBytes = banditResponse.getBody();
        }
      } catch (InterruptedException e) {
        log.error("Error fetching bandit parameters", e);
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        log.error("Error fetching bandit parameters", e);
        throw new RuntimeException(e);
      }
    }

    ConfigurationType config =
        configurationParser.buildConfig(
            flagConfigResponse, flagResponse.getVersionId(), lastConfig, banditBytes);
    configurationStore.saveConfiguration(config).join();
  }

  private boolean needsFreshBandits(FlagConfigResponse flagResponse, ConfigurationType lastConfig) {
    if (!supportBandits) return false;
    if (flagResponse.getBanditReferences() == null
        || flagResponse.getBanditReferences().isEmpty()) {
      return false;
    }
    return flagResponse.getBanditReferences().entrySet().stream()
        .anyMatch(
            entry -> {
              BanditParameters loaded = lastConfig.getBanditParameters(entry.getKey());
              return loaded == null
                  || !entry.getValue().getModelVersion().equals(loaded.getModelVersion());
            });
  }

  /** Loads configuration asynchronously from the API server, off-thread. */
  @Override
  public CompletableFuture<Void> fetchAndSaveFromRemoteAsync() {
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

    byte[] flagBody = flagResponse.getBody();
    if (flagBody == null) {
      throw new RuntimeException("Flag configuration response body is null");
    }
    FlagConfigResponse flagConfigResponse;
    try {
      flagConfigResponse = configurationParser.parseFlagConfig(flagBody);
    } catch (ConfigurationParseException e) {
      log.error("Failed to parse flag configuration", e);
      throw new RuntimeException(e);
    }

    if (needsFreshBandits(flagConfigResponse, lastConfig)) {
      EppoConfigurationRequest banditRequest = requestFactory.createBanditParamsRequest();
      return configurationClient
          .execute(banditRequest)
          .thenCompose(
              banditResponse -> {
                byte[] banditBytes =
                    banditResponse.isSuccessful() && banditResponse.getBody() != null
                        ? banditResponse.getBody()
                        : null;
                ConfigurationType config =
                    configurationParser.buildConfig(
                        flagConfigResponse, flagResponse.getVersionId(), lastConfig, banditBytes);
                return configurationStore.saveConfiguration(config);
              });
    }

    ConfigurationType config =
        configurationParser.buildConfig(
            flagConfigResponse, flagResponse.getVersionId(), lastConfig, null);
    return configurationStore.saveConfiguration(config);
  }
}

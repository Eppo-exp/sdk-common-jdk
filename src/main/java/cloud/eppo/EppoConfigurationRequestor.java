package cloud.eppo;

import cloud.eppo.api.SerializableEppoConfiguration;
import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.BanditReference;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.http.EppoConfigurationClient;
import cloud.eppo.http.EppoConfigurationRequest;
import cloud.eppo.http.EppoConfigurationRequestFactory;
import cloud.eppo.http.EppoConfigurationResponse;
import cloud.eppo.parser.ConfigurationParseException;
import cloud.eppo.parser.ConfigurationParser;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
  private final Object saveLock = new Object();
  // Guarded by saveLock. These reservations are private: cancelling a caller's future must not
  // release ownership of a configuration already admitted for persistence.
  private boolean initialConfigurationRegistered = false;
  private int pendingRemoteSaves = 0;
  private CompletableFuture<Void> remoteSavesDrained = CompletableFuture.completedFuture(null);
  private boolean initialSavePending = false;
  private boolean submittingDeferredRemoteSaves = false;
  private final Deque<Runnable> deferredRemoteSaves = new ArrayDeque<>();
  private volatile boolean fetchedConfigSaved = false;

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
    Objects.requireNonNull(configurationFuture, "configurationFuture");
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    synchronized (saveLock) {
      if (initialConfigurationRegistered) {
        throw new IllegalStateException("Configuration future has already been set");
      }
      // Reserve registration before attaching a continuation to an already-completed input.
      initialConfigurationRegistered = true;
    }
    configurationFuture
        .thenCompose(this::saveInitialConfiguration)
        .whenComplete(
            (saved, failure) -> {
              if (failure != null) {
                log.error("Error setting initial config", failure);
                result.complete(false);
              } else {
                result.complete(saved);
              }
            });
    return result.thenApply(saved -> saved);
  }

  private CompletableFuture<Boolean> saveInitialConfiguration(ConfigurationType config) {
    if (config == null || config.isEmpty()) {
      return CompletableFuture.completedFuture(false);
    }
    CompletableFuture<Void> pending;
    synchronized (saveLock) {
      if (fetchedConfigSaved) {
        return CompletableFuture.completedFuture(false);
      }
      pending = pendingRemoteSaves > 0 ? remoteSavesDrained : null;
      if (pending == null) {
        initialSavePending = true;
      }
    }
    if (pending != null) {
      // Another remote save may have started while this batch drained. Recheck before admission.
      return pending.thenCompose(ignored -> saveInitialConfiguration(config));
    }

    CompletableFuture<Boolean> result = new CompletableFuture<>();
    try {
      configurationStore
          .saveConfiguration(config)
          .whenComplete(
              (ignored, failure) -> {
                finishInitialSave();
                if (failure == null) {
                  result.complete(true);
                } else {
                  result.completeExceptionally(failure);
                }
              });
    } catch (Throwable failure) {
      finishInitialSave();
      result.completeExceptionally(failure);
    }
    return result;
  }

  private void finishInitialSave() {
    synchronized (saveLock) {
      initialSavePending = false;
      submittingDeferredRemoteSaves = true;
    }
    while (true) {
      Runnable next;
      synchronized (saveLock) {
        next = deferredRemoteSaves.pollFirst();
        if (next == null) {
          submittingDeferredRemoteSaves = false;
          return;
        }
      }
      // Preserve admission order without waiting for earlier remote persistence to finish.
      next.run();
    }
  }

  private CompletableFuture<Void> saveRemoteConfiguration(ConfigurationType config) {
    CompletableFuture<Void> result = new CompletableFuture<>();
    boolean deferred;
    synchronized (saveLock) {
      if (pendingRemoteSaves++ == 0) {
        remoteSavesDrained = new CompletableFuture<>();
      }
      deferred = initialSavePending || submittingDeferredRemoteSaves;
      if (deferred) {
        // One drain preserves FIFO order; independent future callbacks may run in reverse order.
        deferredRemoteSaves.addLast(() -> persistRemoteConfiguration(config, result));
      }
    }
    if (!deferred) {
      persistRemoteConfiguration(config, result);
    }
    return result.thenApply(ignored -> null);
  }

  private void persistRemoteConfiguration(
      ConfigurationType config, CompletableFuture<Void> result) {
    try {
      configurationStore
          .saveConfiguration(config)
          .whenComplete((ignored, failure) -> finishRemoteSave(result, failure));
    } catch (Throwable failure) {
      finishRemoteSave(result, failure);
    }
  }

  private void finishRemoteSave(CompletableFuture<Void> result, @Nullable Throwable failure) {
    CompletableFuture<Void> drained = null;
    synchronized (saveLock) {
      if (failure == null) {
        fetchedConfigSaved = true;
      }
      if (--pendingRemoteSaves == 0) {
        drained = remoteSavesDrained;
      }
    }
    // Completing a future may call the store or subscribers again. Never do so under saveLock.
    if (drained != null) {
      drained.complete(null);
    }
    if (failure == null) {
      result.complete(null);
    } else {
      result.completeExceptionally(failure);
    }
  }

  /** Loads configuration synchronously from the API server. */
  @Override
  public void fetchAndSaveFromRemote() {
    log.debug("Fetching configuration");

    // Reuse the lastConfig as its bandits may be useful
    ConfigurationType lastConfig = configurationStore.getConfiguration();

    EppoConfigurationRequest flagRequest =
        requestFactory.createFlagConfigRequest(etagToSend(lastConfig));
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

    byte[] banditBytes =
        needsFreshBandits(flagConfigResponse, lastConfig) ? fetchBanditParameters() : null;
    ConfigurationType config =
        buildConfigWithBanditCheck(
            flagConfigResponse, flagResponse.getVersionId(), lastConfig, banditBytes);
    saveRemoteConfiguration(config).join();
  }

  // Until this requestor saves a fetched configuration, it ignores the saved ETag. A configuration
  // that was saved without its bandit models is then downloaded again instead of getting a 304.
  @Nullable private String etagToSend(ConfigurationType lastConfig) {
    return fetchedConfigSaved ? lastConfig.getFlagsSnapshotId() : null;
  }

  // A failed bandit fetch returns null, so the save keeps the loaded models and flag updates still
  // apply. An interrupt still aborts the poll.
  @Nullable private byte[] fetchBanditParameters() {
    EppoConfigurationRequest banditRequest = requestFactory.createBanditParamsRequest();
    try {
      return requireBanditBody(configurationClient.execute(banditRequest).get());
    } catch (InterruptedException e) {
      log.error("Bandit parameter fetch interrupted", e);
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException | RuntimeException e) {
      log.warn("Failed to fetch bandit parameters; keeping the loaded models: {}", describe(e));
      log.debug("Bandit parameter fetch failure", e);
      return null;
    }
  }

  // Builds the config, keeping the loaded models if the bandit bytes fail to parse. While a
  // referenced model version is not loaded, drops the ETag so the next poll gets the flags again
  // instead of a 304 and retries the bandits.
  private ConfigurationType buildConfigWithBanditCheck(
      FlagConfigResponse flagConfigResponse,
      @Nullable String flagsSnapshotId,
      ConfigurationType lastConfig,
      @Nullable byte[] banditBytes) {
    ConfigurationType config;
    try {
      config =
          configurationParser.buildConfig(
              flagConfigResponse, flagsSnapshotId, lastConfig, banditBytes);
    } catch (RuntimeException e) {
      if (banditBytes == null) {
        throw e;
      }
      log.warn("Failed to parse bandit parameters; keeping the loaded models: {}", describe(e));
      log.debug("Bandit parameter parse failure", e);
      banditBytes = null;
      config =
          configurationParser.buildConfig(flagConfigResponse, flagsSnapshotId, lastConfig, null);
    }
    if (flagsSnapshotId != null && needsFreshBandits(flagConfigResponse, config)) {
      log.warn(
          "Loaded bandit models do not match the flags: {}; saving without the ETag so the next"
              + " poll retries them",
          staleBandits(flagConfigResponse, config));
      return configurationParser.buildConfig(flagConfigResponse, null, lastConfig, banditBytes);
    }
    return config;
  }

  // Names the failure without the wrapper that a future adds. A bandit outage can log this on every
  // poll, so the stack trace goes to DEBUG.
  private static String describe(Throwable e) {
    Throwable cause =
        (e instanceof CompletionException || e instanceof ExecutionException)
                && e.getCause() != null
            ? e.getCause()
            : e;
    return cause.toString();
  }

  private static byte[] requireBanditBody(EppoConfigurationResponse banditResponse) {
    if (!banditResponse.isSuccessful()) {
      throw new RuntimeException(
          "Failed to fetch bandit parameters. Status: " + banditResponse.getStatusCode());
    }
    byte[] banditBody = banditResponse.getBody();
    if (banditBody == null) {
      throw new RuntimeException("Bandit parameters response body is null");
    }
    return banditBody;
  }

  private boolean needsFreshBandits(FlagConfigResponse flagResponse, ConfigurationType lastConfig) {
    if (!supportBandits) return false;
    if (flagResponse.getBanditReferences() == null
        || flagResponse.getBanditReferences().isEmpty()) {
      return false;
    }
    return flagResponse.getBanditReferences().entrySet().stream()
        .anyMatch(entry -> isStale(entry, lastConfig));
  }

  private boolean isStale(Map.Entry<String, BanditReference> reference, ConfigurationType config) {
    BanditParameters loaded = config.getBanditParameters(reference.getKey());
    return loaded == null
        || !reference.getValue().getModelVersion().equals(loaded.getModelVersion());
  }

  private String staleBandits(FlagConfigResponse flagResponse, ConfigurationType config) {
    return flagResponse.getBanditReferences().entrySet().stream()
        .filter(entry -> isStale(entry, config))
        .map(
            entry -> {
              BanditParameters loaded = config.getBanditParameters(entry.getKey());
              return entry.getKey()
                  + " (flags want "
                  + entry.getValue().getModelVersion()
                  + ", loaded "
                  + (loaded == null ? "none" : loaded.getModelVersion())
                  + ")";
            })
        .collect(Collectors.joining(", ", "[", "]"));
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
        requestFactory.createFlagConfigRequest(etagToSend(lastConfig));

    CompletableFuture<Void> fetchFuture =
        configurationClient
            .execute(flagRequest)
            .thenCompose(
                flagResponse -> {
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
                });
    remoteFetchFuture = fetchFuture;
    return fetchFuture;
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
      // thenCompose turns an exception that execute throws into a failed future for the fallback.
      return CompletableFuture.completedFuture(banditRequest)
          .thenCompose(configurationClient::execute)
          .thenApply(EppoConfigurationRequestor::requireBanditBody)
          .exceptionally(
              e -> {
                log.warn(
                    "Failed to fetch bandit parameters; keeping the loaded models: {}",
                    describe(e));
                log.debug("Bandit parameter fetch failure", e);
                return null;
              })
          .thenCompose(
              banditBytes ->
                  saveRemoteConfiguration(
                      buildConfigWithBanditCheck(
                          flagConfigResponse,
                          flagResponse.getVersionId(),
                          lastConfig,
                          banditBytes)));
    }

    ConfigurationType config =
        buildConfigWithBanditCheck(
            flagConfigResponse, flagResponse.getVersionId(), lastConfig, null);
    return saveRemoteConfiguration(config);
  }
}

package cloud.eppo;

import cloud.eppo.api.Configuration;
import cloud.eppo.callback.CallbackManager;
import cloud.eppo.http.EppoConfigurationRequestFactory;
import cloud.eppo.http.EppoHttpClient;
import cloud.eppo.http.EppoHttpRequest;
import cloud.eppo.http.EppoHttpResponse;
import cloud.eppo.parser.ConfigurationParser;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final EppoHttpClient client;
  private final ConfigurationParser parser;
  private final EppoConfigurationRequestFactory requestFactory;
  private final IConfigurationStore configurationStore;
  private final boolean supportBandits;

  private CompletableFuture<Void> remoteFetchFuture = null;
  private CompletableFuture<Boolean> configurationFuture = null;
  private boolean initialConfigSet = false;

  private final CallbackManager<Configuration> configChangeManager = new CallbackManager<>();

  public ConfigurationRequestor(
      @NotNull IConfigurationStore configurationStore,
      @NotNull EppoHttpClient client,
      @NotNull ConfigurationParser parser,
      @NotNull EppoConfigurationRequestFactory requestFactory,
      boolean supportBandits) {
    this.configurationStore = configurationStore;
    this.client = client;
    this.parser = parser;
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

    // Reuse the `lastConfig` as its bandits may be useful
    Configuration lastConfig = configurationStore.getConfiguration();

    // Build request with conditional fetch support (If-None-Match)
    String etag = (lastConfig != null) ? lastConfig.getFlagsVersionId() : null;
    EppoHttpRequest flagRequest = requestFactory.createFlagConfigRequest(etag);

    EppoHttpResponse flagResponse;
    try {
      flagResponse = client.get(flagRequest).get();
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error fetching flag config: " + e.getMessage());
      throw new RuntimeException(e);
    }

    // If not modified, keep existing config
    if (flagResponse.isNotModified()) {
      log.debug("Flag configuration not modified (304)");
      return;
    }

    if (!flagResponse.isSuccessful()) {
      log.error("Failed to fetch flag config: status {}", flagResponse.getStatusCode());
      throw new RuntimeException(
          "Failed to fetch flag config: status " + flagResponse.getStatusCode());
    }

    byte[] flagConfigurationJsonBytes = flagResponse.getBody();
    Configuration.Builder configBuilder =
        Configuration.builder(flagConfigurationJsonBytes, parser)
            .banditParametersFromConfig(lastConfig)
            .flagsVersionId(flagResponse.getEtag());

    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
      EppoHttpRequest banditRequest = requestFactory.createBanditParamsRequest();
      EppoHttpResponse banditResponse;
      try {
        banditResponse = client.get(banditRequest).get();
      } catch (InterruptedException | ExecutionException e) {
        log.error("Error fetching bandit params: " + e.getMessage());
        throw new RuntimeException(e);
      }
      if (banditResponse.isSuccessful()) {
        configBuilder.banditParameters(banditResponse.getBody());
      }
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

    // Build request with conditional fetch support (If-None-Match)
    String etag = (lastConfig != null) ? lastConfig.getFlagsVersionId() : null;
    EppoHttpRequest flagRequest = requestFactory.createFlagConfigRequest(etag);

    remoteFetchFuture =
        client
            .get(flagRequest)
            .thenCompose(
                flagResponse -> {
                  synchronized (this) {
                    // If not modified, keep existing config
                    if (flagResponse.isNotModified()) {
                      log.debug("Flag configuration not modified (304)");
                      return CompletableFuture.completedFuture(null);
                    }

                    if (!flagResponse.isSuccessful()) {
                      log.error(
                          "Failed to fetch flag config: status {}", flagResponse.getStatusCode());
                      throw new RuntimeException(
                          "Failed to fetch flag config: status " + flagResponse.getStatusCode());
                    }

                    byte[] flagConfigJsonBytes = flagResponse.getBody();
                    Configuration.Builder configBuilder =
                        Configuration.builder(flagConfigJsonBytes, parser)
                            .banditParametersFromConfig(lastConfig)
                            .flagsVersionId(flagResponse.getEtag());

                    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
                      EppoHttpRequest banditRequest = requestFactory.createBanditParamsRequest();
                      EppoHttpResponse banditResponse;
                      try {
                        banditResponse = client.get(banditRequest).get();
                      } catch (InterruptedException | ExecutionException e) {
                        log.error("Error fetching bandit params: " + e.getMessage());
                        throw new RuntimeException(e);
                      }
                      if (banditResponse.isSuccessful()) {
                        configBuilder.banditParameters(banditResponse.getBody());
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

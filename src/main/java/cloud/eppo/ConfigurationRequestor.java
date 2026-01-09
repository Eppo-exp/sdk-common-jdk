package cloud.eppo;

import cloud.eppo.api.CallbackAdapter;
import cloud.eppo.api.Configuration;
import cloud.eppo.api.IHttpClient;
import cloud.eppo.callback.CallbackManager;
import cloud.eppo.exception.FetchException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final IHttpClient httpClient;
  private final String apiKey;
  private final String sdkName;
  private final String sdkVersion;
  private final IConfigurationStore configurationStore;
  private final boolean expectObfuscatedConfig;
  private final boolean supportBandits;

  private CompletableFuture<Void> remoteFetchFuture = null;
  private CompletableFuture<Boolean> configurationFuture = null;
  private boolean initialConfigSet = false;

  private final CallbackManager<Configuration> configChangeManager = new CallbackManager<>();

  public ConfigurationRequestor(
      @NotNull IConfigurationStore configurationStore,
      @NotNull IHttpClient httpClient,
      @NotNull String apiKey,
      @NotNull String sdkName,
      @NotNull String sdkVersion,
      boolean expectObfuscatedConfig,
      boolean supportBandits) {
    this.configurationStore = configurationStore;
    this.httpClient = httpClient;
    this.apiKey = apiKey;
    this.sdkName = sdkName;
    this.sdkVersion = sdkVersion;
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

    try {
      // Reuse the `lastConfig` as its bandits may be useful
      Configuration lastConfig = configurationStore.getConfiguration();
      Map<String, String> queryParams = buildQueryParams();

      byte[] flagConfigurationJsonBytes =
          httpClient.fetch(Constants.FLAG_CONFIG_ENDPOINT, queryParams);
      // Builder will auto-detect obfuscation from the format field in the response
      Configuration.Builder configBuilder =
          Configuration.builder(flagConfigurationJsonBytes).banditParametersFromConfig(lastConfig);

      if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
        byte[] banditParametersJsonBytes = httpClient.fetch(Constants.BANDIT_ENDPOINT, queryParams);
        configBuilder.banditParameters(banditParametersJsonBytes);
      }

      saveConfigurationAndNotify(configBuilder.build()).join();
    } catch (FetchException e) {
      log.error("Failed to fetch configuration", e);
      throw new RuntimeException("Configuration fetch failed", e);
    }
  }

  /** Loads configuration asynchronously from the API server, off-thread. */
  CompletableFuture<Void> fetchAndSaveFromRemoteAsync() {
    log.debug("Fetching configuration from API server");
    final Configuration lastConfig = configurationStore.getConfiguration();
    final Map<String, String> queryParams = buildQueryParams();

    if (remoteFetchFuture != null && !remoteFetchFuture.isDone()) {
      log.debug("Remote fetch is active. Cancelling and restarting");
      remoteFetchFuture.cancel(true);
      remoteFetchFuture = null;
    }

    remoteFetchFuture =
        CallbackAdapter.<byte[]>toFuture(
                callback ->
                    httpClient.fetchAsync(Constants.FLAG_CONFIG_ENDPOINT, queryParams, callback))
            .thenCompose(
                flagConfigJsonBytes -> {
                  synchronized (this) {
                    Configuration.Builder configBuilder =
                        Configuration.builder(flagConfigJsonBytes)
                            .banditParametersFromConfig(
                                lastConfig); // possibly reuse last bandit models loaded.

                    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
                      return fetchBanditParameters(queryParams)
                          .thenApply(
                              banditParametersJsonBytes -> {
                                if (banditParametersJsonBytes != null) {
                                  configBuilder.banditParameters(banditParametersJsonBytes);
                                }
                                return configBuilder;
                              });
                    } else {
                      return CompletableFuture.completedFuture(configBuilder);
                    }
                  }
                })
            .thenCompose(
                configBuilder -> {
                  return saveConfigurationAndNotify(configBuilder.build());
                });
    return remoteFetchFuture;
  }

  private CompletableFuture<byte[]> fetchBanditParameters(Map<String, String> queryParams) {
    return CallbackAdapter.<byte[]>toFuture(
        callback -> httpClient.fetchAsync(Constants.BANDIT_ENDPOINT, queryParams, callback));
  }

  private Map<String, String> buildQueryParams() {
    Map<String, String> params = new HashMap<>();
    params.put("apiKey", apiKey);
    params.put("sdkName", sdkName);
    params.put("sdkVersion", sdkVersion);
    return params;
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

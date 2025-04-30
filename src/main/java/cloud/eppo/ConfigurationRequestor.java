package cloud.eppo;

import cloud.eppo.api.Configuration;
import cloud.eppo.api.EppoActionCallback;
import cloud.eppo.callback.CallbackManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final IEppoHttpClient client;
  private final IConfigurationStore configurationStore;
  private final boolean supportBandits;

  private final CallbackManager<Configuration, Configuration.Callback> configChangeManager =
      new CallbackManager<>(
          // no lambdas before java8
          new CallbackManager.Dispatcher<Configuration, Configuration.Callback>() {
            @Override
            public void dispatch(Configuration.Callback callback, Configuration data) {
              callback.accept(data);
            }
          });

  public ConfigurationRequestor(
      @NotNull IConfigurationStore configurationStore,
      @NotNull IEppoHttpClient client,
      boolean supportBandits) {
    this.configurationStore = configurationStore;
    this.client = client;
    this.supportBandits = supportBandits;
  }

  /**
   * Synchronously sets and activates the initial configuration.
   *
   * @param configuration The configuration to activate
   */
  public void activateConfiguration(@NotNull Configuration configuration) {
    saveConfigurationAndNotify(configuration);
  }

  /** Loads configuration synchronously from the API server. */
  void fetchAndSaveFromRemote() {
    log.debug("Fetching configuration");

    // Reuse the `lastConfig` as its bandits may be useful
    Configuration lastConfig = configurationStore.getConfiguration();

    byte[] flagConfigurationJsonBytes = client.get(Constants.FLAG_CONFIG_ENDPOINT);
    Configuration.Builder configBuilder =
        Configuration.builder(flagConfigurationJsonBytes).banditParametersFromConfig(lastConfig);

    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
      byte[] banditParametersJsonBytes = client.get(Constants.BANDIT_ENDPOINT);
      configBuilder.banditParameters(banditParametersJsonBytes);
    }

    saveConfigurationAndNotify(configBuilder.build());
  }

  /** Loads configuration asynchronously from the API server, off-thread. */
  void fetchAndSaveFromRemoteAsync(EppoActionCallback<Configuration> callback) {
    log.debug("Fetching configuration from API server");
    final Configuration lastConfig = configurationStore.getConfiguration();

    client.getAsync(
        Constants.FLAG_CONFIG_ENDPOINT,
        new IEppoHttpClient.EppoHttpCallback() {
          @Override
          public void onSuccess(byte[] flagConfigJsonBytes) {
            synchronized (this) {
              Configuration.Builder configBuilder =
                  Configuration.builder(flagConfigJsonBytes)
                      .banditParametersFromConfig(
                          lastConfig); // possibly reuse last bandit models loaded.

              if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
                byte[] banditParametersJsonBytes;

                banditParametersJsonBytes = client.get(Constants.BANDIT_ENDPOINT);

                if (banditParametersJsonBytes != null) {
                  configBuilder.banditParameters(banditParametersJsonBytes);
                }
              }

              Configuration config = configBuilder.build();
              saveConfigurationAndNotify(config);
              callback.onSuccess(config);
            }
          }

          @Override
          public void onFailure(Throwable error) {
            log.error(
                "Failed to fetch configuration from API server: {}", error.getMessage(), error);
            callback.onFailure(error);
          }
        });
  }

  private void saveConfigurationAndNotify(Configuration configuration) {
    configurationStore.saveConfiguration(configuration);
    synchronized (configChangeManager) {
      configChangeManager.notifyCallbacks(configuration);
    }
  }

  public Runnable onConfigurationChange(Configuration.Callback callback) {
    return configChangeManager.subscribe(callback);
  }
}

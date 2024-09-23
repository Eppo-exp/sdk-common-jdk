package cloud.eppo;

import cloud.eppo.api.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);
  public static final String FLAG_CONFIG_PATH = "/api/flag-config/v1/config";
  public static final String BANDIT_PARAMETER_PATH = "/api/flag-config/v1/bandits";

  private final EppoHttpClient client;
  private final boolean expectObfuscatedConfig;
  private final boolean supportBandits;

  ConfigurationRequestor(EppoHttpClient client, boolean expectObfuscatedConfig) {
    this(client, expectObfuscatedConfig, true);
  }

  ConfigurationRequestor(
      EppoHttpClient client, boolean expectObfuscatedConfig, boolean supportBandits) {
    this.client = client;
    this.expectObfuscatedConfig = expectObfuscatedConfig;
    this.supportBandits = supportBandits;
  }

  /**
   * Loads configuration synchronously from the API server.
   *
   * @param lastConfig Currently loaded Bandit Parameters can be reused if they satisfy the models
   *     referenced by flags.
   */
  Configuration loadFromRemote(Configuration lastConfig) {
    // Reuse the `lastConfig` as its bandits may be useful
    log.debug("Fetching configuration");
    byte[] flagConfigurationJsonBytes = client.get(FLAG_CONFIG_PATH);
    Configuration.Builder configBuilder =
        new Configuration.Builder(flagConfigurationJsonBytes, expectObfuscatedConfig)
            .banditParametersFromConfig(lastConfig);

    if (supportBandits && configBuilder.requiresBanditModels()) {
      byte[] banditParametersJsonBytes = client.get(BANDIT_PARAMETER_PATH);
      configBuilder.banditParameters(banditParametersJsonBytes);
    }

    return configBuilder.build();
  }

  /**
   * Loads configuration asynchronously from the API server, off-thread.
   *
   * @param lastConfig Currently loaded Bandit Parameters can be reused if they satisfy the models
   *     referenced by flags.
   * @param callback executed upon completion or failure of the load.
   */
  void loadFromRemote(final Configuration lastConfig, final ConfigurationCallback callback) {
    log.debug("Fetching configuration from API server");
    client.get(
        FLAG_CONFIG_PATH,
        new EppoHttpClient.RequestCallback() {

          @Override
          public void onSuccess(byte[] flagConfigurationJsonBytes) {
            Configuration.Builder configBuilder =
                new Configuration.Builder(flagConfigurationJsonBytes, expectObfuscatedConfig)
                    .banditParametersFromConfig(
                        lastConfig); // possibly reuse last bandit models loaded for efficiency.

            if (supportBandits && configBuilder.requiresBanditModels()) {
              client.get(
                  BANDIT_PARAMETER_PATH,
                  new EppoHttpClient.RequestCallback() {

                    @Override
                    public void onSuccess(byte[] banditParametersJsonBytes) {
                      configBuilder.banditParameters(banditParametersJsonBytes);
                      callback.onSuccess(configBuilder.build());
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                      log.error(errorMessage);
                      callback.onFailure(errorMessage);
                    }
                  });
            } else {
              // Bandit loading not necessary, so we complete the callback.
              callback.onSuccess(configBuilder.build());
            }
          }

          @Override
          public void onFailure(String errorMessage) {
            callback.onFailure(errorMessage);
          }
        });
  }

  interface ConfigurationCallback extends EppoCallback<Configuration> {}
}

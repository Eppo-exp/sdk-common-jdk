package cloud.eppo;

import cloud.eppo.api.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: handle bandit stuff
public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final EppoHttpClient client;
  private final IConfigurationStore configurationStore;
  private final boolean expectObfuscatedConfig;

  public ConfigurationRequestor(
      IConfigurationStore configurationStore,
      EppoHttpClient client,
      boolean expectObfuscatedConfig) {
    this.configurationStore = configurationStore;
    this.client = client;
    this.expectObfuscatedConfig = expectObfuscatedConfig;
  }

  // TODO: async loading for android
  public void load() {
    // Grab hold of the last configuration in case its bandit models are useful
    Configuration lastConfig = configurationStore.getConfiguration();

    log.debug("Fetching configuration");
    byte[] flagConfigurationJsonBytes = client.get("/api/flag-config/v1/config");
    Configuration.Builder configBuilder =
        new Configuration.Builder(flagConfigurationJsonBytes, expectObfuscatedConfig)
            .banditParametersFromConfig(lastConfig);

    if (configBuilder.requiresBanditModels()) {
      byte[] banditParametersJsonBytes = client.get("/api/flag-config/v1/bandits");
      configBuilder.banditParameters(banditParametersJsonBytes);
    }

    configurationStore.setConfiguration(configBuilder.build());
  }
}

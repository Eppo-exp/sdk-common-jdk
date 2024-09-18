package cloud.eppo;

import java.io.IOException;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: handle bandit stuff
public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final EppoHttpClient client;
  private final ConfigurationStore configurationStore;
  private final boolean expectObfuscatedConfig;

  public ConfigurationRequestor(
      ConfigurationStore configurationStore,
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
    byte[] flagConfigurationJsonBytes = requestBody("/api/flag-config/v1/config");
    Configuration.Builder configBuilder =
        new Configuration.Builder(flagConfigurationJsonBytes, expectObfuscatedConfig)
            .banditParametersFromConfig(lastConfig);

    if (configBuilder.requiresBanditModels()) {
      byte[] banditParametersJsonBytes = requestBody("/api/flag-config/v1/bandits");
      configBuilder.banditParameters(banditParametersJsonBytes);
    }

    configurationStore.setConfiguration(configBuilder.build());
  }

  private byte[] requestBody(String route) {
    Response response = client.get(route);
    if (!response.isSuccessful() || response.body() == null) {
      throw new RuntimeException("Failed to fetch from " + route);
    }
    try {
      return response.body().bytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

package cloud.eppo;

import java.io.IOException;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: handle bandit stuff
public class ConfigurationRequester {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequester.class);

  private final EppoHttpClient client;
  private final ConfigurationStore configurationStore;

  public ConfigurationRequester(ConfigurationStore configurationStore, EppoHttpClient client) {
    this.configurationStore = configurationStore;
    this.client = client;
  }

  // TODO: async loading for android
  public void load(boolean isConfigObfuscated) {
    // Grab hold of the last configuration in case its bandit models are useful
    Configuration lastConfig = configurationStore.getConfiguration();

    log.debug("Fetching configuration");
    String flagConfigurationJsonString = requestBody("/api/flag-config/v1/config");
    Configuration.Builder configBuilder =
        new Configuration.Builder(flagConfigurationJsonString, isConfigObfuscated)
            .banditParametersFrom(lastConfig);

    if (configBuilder.requiresBanditModels()) {
      String banditParametersJsonString = requestBody("/api/flag-config/v1/bandits");
      configBuilder.banditParameters(banditParametersJsonString);
    }

    configurationStore.setConfiguration(configBuilder.build());
  }

  private String requestBody(String route) {
    Response response = client.get(route);
    if (!response.isSuccessful() || response.body() == null) {
      throw new RuntimeException("Failed to fetch from " + route);
    }
    try {
      return response.body().string();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

package cloud.eppo;

import cloud.eppo.ufc.dto.FlagConfig;
import java.io.IOException;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: handle bandit stuff
public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final EppoHttpClient client;
  private final ConfigurationStore configurationStore;

  public ConfigurationRequestor(ConfigurationStore configurationStore, EppoHttpClient client) {
    this.configurationStore = configurationStore;
    this.client = client;
  }

  public void load() {
    log.debug("Fetching configuration");
    Response response = client.get("/api/flag-config/v1/config");
    try {
      if (!response.isSuccessful()) {
        throw new RuntimeException("Failed to fetch configuration");
      }
      configurationStore.setFlagsFromJsonString(response.body().string());
    } catch (IOException e) {
      // TODO: better exception handling?
      throw new RuntimeException(e);
    }
  }

  // TODO: async loading for android

  public FlagConfig getConfiguration(String flagKey) {
    return configurationStore.getFlag(flagKey);
  }
}

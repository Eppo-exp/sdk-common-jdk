package cloud.eppo;


import cloud.eppo.ufc.dto.FlagConfig;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

// TODO: handle bandit stuff
public class ConfigurationRequestor<T> {
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
      // TODO: make sure response succeeded?
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

package cloud.eppo;

import cloud.eppo.ufc.dto.FlagConfig;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: handle bandit stuff
public class ConfigurationRequestor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);

  private final EppoHttpClient client;
  private final ConfigurationStore configurationStore;
  private final Set<String> loadedBanditModelVersions;

  public ConfigurationRequestor(ConfigurationStore configurationStore, EppoHttpClient client) {
    this.configurationStore = configurationStore;
    this.client = client;
    this.loadedBanditModelVersions = new HashSet<>();
  }

  // TODO: async loading for android
  public void load() {
    log.debug("Fetching configuration");
    String flagConfigurationJsonString = requestBody("/api/flag-config/v1/config");
    configurationStore.setFlagsFromJsonString(flagConfigurationJsonString);

    Set<String> neededModelVersions = configurationStore.banditModelVersions();
    boolean needBanditParameters = !loadedBanditModelVersions.containsAll(neededModelVersions);
    if (needBanditParameters) {
      String banditParametersJsonString = requestBody("/api/flag-config/v1/bandits");
      configurationStore.setBanditParametersFromJsonString(banditParametersJsonString);
      // Record the model versions that we just loaded, so we can compare when the store is later
      // updated
      loadedBanditModelVersions.clear();
      loadedBanditModelVersions.addAll(configurationStore.banditModelVersions());
    }
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

  public FlagConfig getConfiguration(String flagKey) {
    return configurationStore.getFlag(flagKey);
  }
}

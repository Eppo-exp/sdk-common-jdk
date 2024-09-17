package cloud.eppo;

import cloud.eppo.configuration.ConfigurationBuffer;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: handle bandit stuff
public class ConfigurationRequester {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequester.class);

  private final EppoHttpClient client;
  private final ConfigurationStore configurationStore;
  private final Set<String> loadedBanditModelVersions;

  public ConfigurationRequester(ConfigurationStore configurationStore, EppoHttpClient client) {
    this.configurationStore = configurationStore;
    this.client = client;
    this.loadedBanditModelVersions = new HashSet<>();
  }

  // TODO: async loading for android
  public void load(boolean isConfigObfuscated) {
    log.debug("Fetching configuration");
    String flagConfigurationJsonString = requestBody("/api/flag-config/v1/config");
    ConfigurationBuffer buffer =
        new ConfigurationBuffer(flagConfigurationJsonString, isConfigObfuscated);

    Set<String> neededModelVersions = buffer.referencedBanditModelVersion();
    boolean needBanditParameters = !loadedBanditModelVersions.containsAll(neededModelVersions);
    if (needBanditParameters) {
      String banditParametersJsonString = requestBody("/api/flag-config/v1/bandits");
      buffer.setBandits(banditParametersJsonString);
      // Record the model versions that we just loaded, so we can compare when the store is later
      // updated
      loadedBanditModelVersions.clear();
      loadedBanditModelVersions.addAll(buffer.loadedBanditModelVersions());
    }

    configurationStore.setConfiguration(buffer);
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

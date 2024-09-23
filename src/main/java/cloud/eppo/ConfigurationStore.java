package cloud.eppo;

import cloud.eppo.api.Configuration;

/** In-memory store of a configuration snapshot. */
public class ConfigurationStore implements IConfigurationStore {

  private volatile Configuration configuration = null;

  public ConfigurationStore() {}

  @Override
  public void saveConfiguration(final Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void load(CacheLoadCallback callback) {
    // Immediately return the configuration
    callback.onSuccess(configuration);
  }
}

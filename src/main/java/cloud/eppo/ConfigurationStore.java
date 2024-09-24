package cloud.eppo;

import cloud.eppo.api.Configuration;
import org.jetbrains.annotations.NotNull;

public class ConfigurationStore implements IConfigurationStore {

  private volatile Configuration configuration;

  public ConfigurationStore() {
    configuration = null;
  }

  public void saveConfiguration(@NotNull final Configuration configuration) {
    this.configuration = configuration;
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}

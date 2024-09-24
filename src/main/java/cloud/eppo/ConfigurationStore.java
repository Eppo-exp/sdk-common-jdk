package cloud.eppo;

import cloud.eppo.api.Configuration;
import org.jetbrains.annotations.NotNull;

public class ConfigurationStore implements IConfigurationStore {

  private volatile Configuration configuration;

  public ConfigurationStore(final Configuration initialConfiguration) {
    if (initialConfiguration != null) {
      configuration = initialConfiguration;
    } else {
      configuration = Configuration.emptyConfig();
    }
  }

  public void saveConfiguration(@NotNull final Configuration configuration) {
    this.configuration = configuration;
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}

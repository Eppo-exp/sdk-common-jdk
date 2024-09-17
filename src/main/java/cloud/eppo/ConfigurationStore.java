package cloud.eppo;

import org.jetbrains.annotations.NotNull;

public class ConfigurationStore {

  private volatile Configuration configuration;

  public ConfigurationStore(final Configuration initialConfiguration) {
    if (initialConfiguration != null) {
      this.configuration = initialConfiguration;
    } else {
      configuration = Configuration.Builder.emptyConfig();
    }
  }

  public void setConfiguration(@NotNull final Configuration configuration) {
    this.configuration = configuration;
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}

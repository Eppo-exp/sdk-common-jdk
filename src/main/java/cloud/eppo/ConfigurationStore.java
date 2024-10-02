package cloud.eppo;

import cloud.eppo.api.Configuration;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/** Memory-only configuration store. */
public class ConfigurationStore implements IConfigurationStore {

  private volatile Configuration configuration;

  public ConfigurationStore() {
    configuration = null;
  }

  public CompletableFuture<Void> saveConfiguration(@NotNull final Configuration configuration) {
    this.configuration = configuration;
    return CompletableFuture.completedFuture(null);
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}

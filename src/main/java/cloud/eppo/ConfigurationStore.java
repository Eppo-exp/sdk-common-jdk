package cloud.eppo;

import cloud.eppo.api.Configuration;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/** Memory-only configuration store. */
public class ConfigurationStore extends AbstractConfigurationStore<Configuration> {

  // this is the fallback value if no configuration is provided (i.e. by fetch or initial config).
  @NotNull private volatile Configuration configuration = Configuration.emptyConfig();

  public ConfigurationStore() {}

  @NotNull @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  protected CompletableFuture<Void> persist(@NotNull final Configuration configuration) {
    this.configuration = configuration;
    return CompletableFuture.completedFuture(null);
  }
}

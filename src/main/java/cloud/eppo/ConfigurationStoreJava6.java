package cloud.eppo;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import cloud.eppo.api.Configuration;

/** Memory-only configuration store. */
public class ConfigurationStoreJava6 implements IConfigurationStoreJava6 {

  // this is the fallback value if no configuration is provided (i.e. by fetch or initial config).
  @NotNull private volatile Configuration configuration = Configuration.emptyConfig();

  public ConfigurationStoreJava6() {}

  public void saveConfigurationJava6(@NotNull final Configuration configuration) {
    this.configuration = configuration;
  }

  @NotNull public Configuration getConfiguration() {
    return configuration;
  }
}

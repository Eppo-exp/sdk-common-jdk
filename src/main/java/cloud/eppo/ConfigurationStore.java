package cloud.eppo;

import cloud.eppo.api.Configuration;
import cloud.eppo.callback.CallbackManager;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/** Memory-only configuration store. */
public class ConfigurationStore implements IConfigurationStore<Configuration> {

  // this is the fallback value if no configuration is provided (i.e. by fetch or initial config).
  @NotNull private volatile Configuration configuration = Configuration.emptyConfig();

  private final CallbackManager<Configuration> callbackManager = new CallbackManager<>();

  public ConfigurationStore() {}

  public CompletableFuture<Void> saveConfiguration(@NotNull final Configuration configuration) {
    this.configuration = configuration;
    callbackManager.notifyCallbacks(configuration);
    return CompletableFuture.completedFuture(null);
  }

  @NotNull public Configuration getConfiguration() {
    return configuration;
  }

  public Runnable subscribe(Consumer<Configuration> callback) {
    return callbackManager.subscribe(callback);
  }

  public boolean unsubscribe(Consumer<Configuration> callback) {
    return callbackManager.unsubscribe(callback);
  }
}

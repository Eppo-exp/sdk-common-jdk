package cloud.eppo;

import cloud.eppo.api.SerializableEppoConfiguration;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public interface ConfigurationRequestor<
    ConfigurationType extends SerializableEppoConfiguration, JsonFlagType> {

  void setInitialConfiguration(@NotNull ConfigurationType configuration);

  CompletableFuture<Boolean> setInitialConfiguration(
      @NotNull CompletableFuture<ConfigurationType> configurationFuture);

  void fetchAndSaveFromRemote();

  CompletableFuture<Void> fetchAndSaveFromRemoteAsync();
}

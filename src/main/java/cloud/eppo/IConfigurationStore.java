package cloud.eppo;

import cloud.eppo.api.Configuration;
import org.jetbrains.annotations.NotNull;

public interface IConfigurationStore {
  void load(CacheLoadCallback callback);

  void saveConfiguration(@NotNull final Configuration configuration);

  interface CacheLoadCallback extends EppoCallback<Configuration> {}
}

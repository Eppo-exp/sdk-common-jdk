package cloud.eppo;

import cloud.eppo.api.Configuration;

public interface IConfigurationStore {
  void load(CacheLoadCallback callback);

  void saveConfiguration(final Configuration configuration);

  interface CacheLoadCallback extends EppoCallback<Configuration> {}
}

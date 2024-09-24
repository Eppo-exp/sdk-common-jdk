package cloud.eppo;

import cloud.eppo.api.Configuration;

public interface IConfigurationStore {
  Configuration getConfiguration();

  void saveConfiguration(Configuration configuration);
}

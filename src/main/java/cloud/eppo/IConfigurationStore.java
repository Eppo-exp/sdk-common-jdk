package cloud.eppo;

import cloud.eppo.api.Configuration;
import org.jetbrains.annotations.NotNull;

/**
 * Common interface for extensions of this SDK to support caching and other strategies for
 * persisting configuration data across sessions.
 */
public interface IConfigurationStore {

  @NotNull Configuration getConfiguration();

  void saveConfiguration(Configuration configuration);
}

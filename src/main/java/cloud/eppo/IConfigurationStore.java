package cloud.eppo;

import cloud.eppo.api.Configuration;
import java.util.concurrent.CompletableFuture;

/**
 * Common interface for extensions of this SDK to support caching and other strategies for
 * persisting configuration data across sessions.
 */
public interface IConfigurationStore {
  Configuration getConfiguration();

  CompletableFuture<Void> saveConfiguration(Configuration configuration);
}

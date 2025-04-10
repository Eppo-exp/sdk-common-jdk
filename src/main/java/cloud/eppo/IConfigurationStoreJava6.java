package cloud.eppo;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import cloud.eppo.api.Configuration;

/**
 * Common interface for extensions of this SDK to support caching and other strategies for
 * persisting configuration data across sessions.
 */
public interface IConfigurationStoreJava6 {
  @NotNull Configuration getConfiguration();

  void saveConfigurationJava6(Configuration configuration);
}

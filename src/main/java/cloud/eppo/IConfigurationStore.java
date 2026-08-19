package cloud.eppo;

import cloud.eppo.api.SerializableEppoConfiguration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * Common interface for extensions of this SDK to support caching and other strategies for
 * persisting configuration data across sessions.
 *
 * <p>Implementations are responsible for storing configuration and notifying subscribers when the
 * configuration changes.
 */
public interface IConfigurationStore<ConfigurationType extends SerializableEppoConfiguration> {
  @NotNull ConfigurationType getConfiguration();

  CompletableFuture<Void> saveConfiguration(ConfigurationType configuration);

  /**
   * Subscribe to configuration change notifications.
   *
   * @param callback invoked with the new configuration each time {@link
   *     #saveConfiguration(SerializableEppoConfiguration)} completes
   * @return a {@link Runnable} that, when called, removes this subscription
   */
  Runnable subscribe(Consumer<ConfigurationType> callback);

  /**
   * Unsubscribe a previously registered callback.
   *
   * @param callback the callback to remove
   * @return {@code true} if the callback was found and removed, {@code false} otherwise
   */
  boolean unsubscribe(Consumer<ConfigurationType> callback);
}

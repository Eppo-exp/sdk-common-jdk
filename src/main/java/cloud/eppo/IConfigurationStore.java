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

  /**
   * Persists the given configuration and notifies any registered subscribers.
   *
   * <p>Implementations must update the stored configuration value <em>before</em> notifying
   * subscribers, so that any subscriber that calls {@link #getConfiguration()} sees the new value.
   *
   * <p>Prefer extending {@link AbstractConfigurationStore} rather than implementing this method
   * directly; it handles locking and subscriber notification automatically.
   */
  CompletableFuture<Void> saveConfiguration(@NotNull ConfigurationType configuration);

  /**
   * Subscribe to configuration change notifications.
   *
   * @param callback invoked with the new configuration each time {@link
   *     #saveConfiguration(SerializableEppoConfiguration)} completes
   * @return a {@link Runnable} that, when called, removes this subscription
   */
  Runnable subscribe(Consumer<ConfigurationType> callback);

  /**
   * Unsubscribes a previously registered callback using identity comparison ({@code ==}).
   *
   * <p>Pass the exact callback reference returned to {@link #subscribe}; implementations compare by
   * identity, not {@code equals}.
   *
   * @param callback the callback to remove
   * @return {@code true} if the callback was found and removed, {@code false} otherwise
   */
  boolean unsubscribe(Consumer<ConfigurationType> callback);
}

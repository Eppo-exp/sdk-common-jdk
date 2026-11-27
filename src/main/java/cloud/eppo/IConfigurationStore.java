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
 * configuration changes. Direct store subscribers are low-level extension hooks; customer observers
 * registered through {@link BaseEppoClient#onConfigurationChange(Consumer)} are delivered
 * independently by the client.
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
   *
   * <p>The returned future must eventually complete, normally after persistence and notification or
   * exceptionally on failure. The requestor waits for an initial save to settle before admitting
   * deferred remote saves. Complete the future only after installing the configuration, and perform
   * blocking storage work asynchronously: a save can be invoked on another save's completion
   * thread.
   *
   * <p>Direct store subscribers run on the store's notification thread and must return promptly.
   * Store implementations and subscribers must not synchronously wait for a configuration fetch
   * that depends on this save completing. For example, remote persistence waits for an initial save
   * to settle; waiting for that fetch inside this save's notification would create a cycle. Start
   * asynchronous work and return from the callback instead.
   */
  CompletableFuture<Void> saveConfiguration(@NotNull ConfigurationType configuration);

  /**
   * Subscribe to low-level configuration change notifications.
   *
   * <p>This extension hook does not provide the independent customer-observer delivery of {@link
   * BaseEppoClient#onConfigurationChange(Consumer)}. Callbacks must be nonblocking and must not
   * wait for dependent configuration operations.
   *
   * @param callback invoked with the new configuration each time {@link
   *     #saveConfiguration(ConfigurationType)} succeeds
   * @return a {@link Runnable} that, when called, removes this subscription
   */
  Runnable subscribe(Consumer<ConfigurationType> callback);

  /**
   * Unsubscribes a previously registered callback using identity comparison ({@code ==}).
   *
   * <p>Pass the exact callback reference supplied to {@link #subscribe}; implementations compare by
   * identity, not {@code equals}.
   *
   * @param callback the callback to remove
   * @return {@code true} if the callback was found and removed, {@code false} otherwise
   */
  boolean unsubscribe(Consumer<ConfigurationType> callback);
}

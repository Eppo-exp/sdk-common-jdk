package cloud.eppo;

import cloud.eppo.api.SerializableEppoConfiguration;
import cloud.eppo.callback.CallbackManager;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience base class for {@link IConfigurationStore} implementations.
 *
 * <p>Owns a {@link CallbackManager} and wires subscriber notification automatically inside {@link
 * #saveConfiguration(SerializableEppoConfiguration)}: subclasses implement only {@link
 * #persist(SerializableEppoConfiguration)}, which handles the actual storage. After {@code persist}
 * completes, all registered subscribers are notified with the new configuration value.
 *
 * <p>{@link #subscribe(Consumer)} and {@link #unsubscribe(Consumer)} delegate to the managed {@code
 * CallbackManager} and do not need to be overridden.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * public class MyStore extends AbstractConfigurationStore<MyConfig> {
 *   private volatile MyConfig config = MyConfig.empty();
 *
 *   @Override
 *   public MyConfig getConfiguration() { return config; }
 *
 *   @Override
 *   protected CompletableFuture<Void> persist(MyConfig configuration) {
 *     this.config = configuration;
 *     return CompletableFuture.completedFuture(null);
 *   }
 * }
 * }</pre>
 */
public abstract class AbstractConfigurationStore<
        ConfigurationType extends SerializableEppoConfiguration>
    implements IConfigurationStore<ConfigurationType> {

  private static final Logger log = LoggerFactory.getLogger(AbstractConfigurationStore.class);

  private static final ThreadLocal<Boolean> inNotification = ThreadLocal.withInitial(() -> false);

  private final CallbackManager<ConfigurationType> callbackManager = new CallbackManager<>();

  /**
   * Persists the configuration and notifies all registered subscribers after the returned future
   * completes.
   *
   * <p>Subclasses must not override this method. Implement {@link
   * #persist(SerializableEppoConfiguration)} instead.
   */
  @Override
  public final CompletableFuture<Void> saveConfiguration(@NotNull ConfigurationType configuration) {
    if (Boolean.TRUE.equals(inNotification.get())) {
      log.debug("saveConfiguration called re-entrantly during notification; ignoring");
      return CompletableFuture.completedFuture(null);
    }
    return persist(configuration)
        .thenRun(
            () -> {
              inNotification.set(true);
              try {
                synchronized (callbackManager) {
                  callbackManager.notifyCallbacks(configuration);
                }
              } finally {
                inNotification.remove();
              }
            });
  }

  /**
   * Store the configuration value. Called by {@link
   * #saveConfiguration(SerializableEppoConfiguration)} before subscribers are notified.
   *
   * <p>Implementations must update the stored value so that a subsequent {@link
   * #getConfiguration()} call returns {@code configuration}.
   */
  protected abstract CompletableFuture<Void> persist(@NotNull ConfigurationType configuration);

  @Override
  public Runnable subscribe(Consumer<ConfigurationType> callback) {
    return callbackManager.subscribe(callback);
  }

  @Override
  public boolean unsubscribe(Consumer<ConfigurationType> callback) {
    return callbackManager.unsubscribe(callback);
  }
}

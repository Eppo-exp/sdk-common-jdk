package cloud.eppo.callback;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic callback manager that allows registration and notification of callbacks.
 *
 * @param <T> The type of data that will be passed to the callbacks
 */
public class CallbackManager<T, C> {
  /**
   * Interface for dispatching data to callbacks.
   *
   * @param <T> The type of data to dispatch
   * @param <C> The type of callback to dispatch to
   */
  public interface Dispatcher<T, C> {
    void dispatch(C callback, T data);
  }

  private final Dispatcher<T, C> dispatcher;

  private static final Logger log = LoggerFactory.getLogger(CallbackManager.class);
  private final Map<String, C> subscribers;

  public CallbackManager(@NotNull Dispatcher<T, C> dispatcher) {
    this.subscribers = new ConcurrentHashMap<>();
    this.dispatcher = dispatcher;
  }

  /**
   * Register a callback to be notified when events occur.
   *
   * @param callback The callback function to be called with event data
   * @return A Runnable that can be called to unsubscribe the callback
   */
  public Runnable subscribe(C callback) {
    String id = UUID.randomUUID().toString();
    subscribers.put(id, callback);

    return new Runnable() {
      @Override
      public void run() {
        subscribers.remove(id);
      }
    };
  }

  /**
   * Notify all subscribers with the provided data.
   *
   * @param data The data to pass to all callbacks
   */
  public void notifyCallbacks(T data) {
    subscribers
        .values()
        .forEach(
            new Consumer<C>() {
              @Override
              public void accept(C callback) {
                try {
                  dispatcher.dispatch(callback, data);
                } catch (Exception e) {
                  log.error("Eppo SDK: Error thrown by callback: {}", e.getMessage());
                }
              }
            });
  }

  /** Remove all subscribers. */
  public void clear() {
    subscribers.clear();
  }
}

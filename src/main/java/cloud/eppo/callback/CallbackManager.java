package cloud.eppo.callback;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic callback manager that allows registration and notification of callbacks.
 *
 * @param <T> The type of data that will be passed to the callbacks
 */
public class CallbackManager<T> {
  private static final Logger log = LoggerFactory.getLogger(CallbackManager.class);
  private final Map<String, Consumer<T>> subscribers;

  public CallbackManager() {
    this.subscribers = new ConcurrentHashMap<>();
  }

  /**
   * Register a callback to be notified when events occur.
   *
   * @param callback The callback function to be called with event data
   * @return A Runnable that can be called to unsubscribe the callback
   */
  public Runnable subscribe(Consumer<T> callback) {
    String id = UUID.randomUUID().toString();
    subscribers.put(id, callback);

    return () -> subscribers.remove(id);
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
            callback -> {
              try {
                callback.accept(data);
              } catch (Exception e) {
                log.error("Eppo SDK: Error thrown by callback: {}", e.getMessage());
              }
            });
  }

  /** Remove all subscribers. */
  public void clear() {
    subscribers.clear();
  }
}

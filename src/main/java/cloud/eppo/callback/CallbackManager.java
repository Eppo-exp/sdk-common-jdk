package cloud.eppo.callback;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A generic callback manager that allows registration and notification of callbacks.
 *
 * @param <T> The type of data that will be passed to the callbacks
 */
public class CallbackManager<T> {
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
  public void notify(T data) {
    subscribers.values().forEach(callback -> callback.accept(data));
  }

  /** Remove all subscribers. */
  public void clear() {
    subscribers.clear();
  }
}

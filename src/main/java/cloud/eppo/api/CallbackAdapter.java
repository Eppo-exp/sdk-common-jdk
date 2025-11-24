package cloud.eppo.api;

import java.util.concurrent.CompletableFuture;

/**
 * Utility class for converting between Callback and CompletableFuture patterns.
 * This allows users who prefer CompletableFuture to easily adapt callback-based APIs.
 */
public final class CallbackAdapter {

  private CallbackAdapter() {
    // Utility class, no instantiation
  }

  /**
   * Converts a Callback-based operation to a CompletableFuture.
   *
   * <p>Usage:
   *
   * <pre>
   * CompletableFuture&lt;byte[]&gt; future = CallbackAdapter.toFuture(
   *     callback -> httpClient.fetchAsync(endpoint, params, callback)
   * );
   * </pre>
   *
   * @param operation Function that accepts a callback and starts the async operation
   * @param <T> The result type
   * @return CompletableFuture that completes when the callback is invoked
   */
  public static <T> CompletableFuture<T> toFuture(CallbackOperation<T> operation) {
    CompletableFuture<T> future = new CompletableFuture<>();

    Callback<T> callback =
        new Callback<T>() {
          @Override
          public void onSuccess(T result) {
            future.complete(result);
          }

          @Override
          public void onError(Throwable error) {
            future.completeExceptionally(error);
          }
        };

    try {
      operation.execute(callback);
    } catch (Exception e) {
      future.completeExceptionally(e);
    }

    return future;
  }

  /**
   * Converts a CompletableFuture to a Callback invocation.
   *
   * <p>Usage:
   *
   * <pre>
   * CompletableFuture&lt;byte[]&gt; future = someAsyncMethod();
   * CallbackAdapter.fromFuture(future, myCallback);
   * </pre>
   *
   * @param future The CompletableFuture to convert
   * @param callback The callback to invoke when the future completes
   * @param <T> The result type
   */
  public static <T> void fromFuture(CompletableFuture<T> future, Callback<T> callback) {
    future.whenComplete(
        (result, error) -> {
          if (error != null) {
            callback.onError(error);
          } else {
            callback.onSuccess(result);
          }
        });
  }

  /**
   * Functional interface for async operations that accept a callback. Used by {@link
   * #toFuture(CallbackOperation)} to bridge callback-style APIs.
   *
   * @param <T> The result type
   */
  @FunctionalInterface
  public interface CallbackOperation<T> {
    /**
     * Execute the async operation with the provided callback.
     *
     * @param callback The callback to invoke when the operation completes
     */
    void execute(Callback<T> callback);
  }
}

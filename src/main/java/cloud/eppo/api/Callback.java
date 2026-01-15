package cloud.eppo.api;

/**
 * Generic callback interface for asynchronous operations. This interface will be used throughout
 * the SDK for async operations including: - HTTP requests - Configuration loading - Assignment
 * logging - Any async operation that can succeed or fail
 *
 * @param <T> The type of the success result
 */
public interface Callback<T> {
  /**
   * Called when the operation completes successfully.
   *
   * @param result The result of the operation
   */
  void onSuccess(T result);

  /**
   * Called when the operation fails.
   *
   * @param error The error that caused the failure
   */
  void onError(Throwable error);
}

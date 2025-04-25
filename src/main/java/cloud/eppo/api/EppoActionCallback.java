package cloud.eppo.api;

/**
 * Interface for handling asynchronous operation results.
 *
 * <p>This callback interface provides methods to handle both successful completion and failure
 * scenarios for asynchronous operations. Implementations should expect that exactly one of the
 * callback methods (either onSuccess or onFailure) will be invoked for each operation.
 *
 * <p>Thread safety: Callbacks may be invoked on any thread, including the calling thread or a
 * background thread. Implementations should be thread-safe and avoid blocking operations.
 *
 * @param <T> The type of data returned on successful completion
 */
public interface EppoActionCallback<T> {
  /**
   * Called when an operation completes successfully.
   *
   * @param data The result data from the operation, may be null in some cases
   */
  void onSuccess(T data);

  /**
   * Called when an operation fails.
   *
   * @param error The error that caused the operation to fail
   */
  void onFailure(Throwable error);
}

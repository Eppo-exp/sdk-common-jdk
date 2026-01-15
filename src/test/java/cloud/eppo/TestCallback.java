package cloud.eppo;

import cloud.eppo.api.Callback;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test helper for callback-based tests. Provides methods to wait for callback completion and access
 * results.
 *
 * @param <T> The result type
 */
public class TestCallback<T> implements Callback<T> {
  private T result;
  private Throwable error;
  private final CountDownLatch latch = new CountDownLatch(1);

  @Override
  public void onSuccess(T result) {
    this.result = result;
    latch.countDown();
  }

  @Override
  public void onError(Throwable error) {
    this.error = error;
    latch.countDown();
  }

  /**
   * Wait for the callback to be invoked and return the result. Throws RuntimeException if the
   * callback received an error.
   *
   * @return The result
   * @throws InterruptedException If the wait is interrupted
   * @throws RuntimeException If the callback received an error
   */
  public T awaitResult() throws InterruptedException {
    return awaitResult(5, TimeUnit.SECONDS);
  }

  /**
   * Wait for the callback to be invoked with a custom timeout.
   *
   * @param timeout The maximum time to wait
   * @param unit The time unit
   * @return The result
   * @throws InterruptedException If the wait is interrupted
   * @throws RuntimeException If the callback received an error or timeout
   */
  public T awaitResult(long timeout, TimeUnit unit) throws InterruptedException {
    if (!latch.await(timeout, unit)) {
      throw new RuntimeException("Callback timeout after " + timeout + " " + unit);
    }
    if (error != null) {
      throw new RuntimeException("Callback received error", error);
    }
    return result;
  }

  /**
   * Get the error if the callback received one.
   *
   * @return The error, or null if the callback was successful
   */
  public Throwable getError() {
    return error;
  }

  /**
   * Get the result without waiting. May be null if callback hasn't been invoked yet.
   *
   * @return The result, or null
   */
  public T getResult() {
    return result;
  }

  /**
   * Check if the callback has been invoked.
   *
   * @return true if the callback was invoked
   */
  public boolean isComplete() {
    return latch.getCount() == 0;
  }
}

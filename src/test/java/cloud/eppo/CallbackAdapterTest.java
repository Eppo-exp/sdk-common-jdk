package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.CallbackAdapter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class CallbackAdapterTest {

  @Test
  public void testToFuture_Success() throws Exception {
    CompletableFuture<String> future =
        CallbackAdapter.toFuture(callback -> callback.onSuccess("test result"));

    assertEquals("test result", future.get(1, TimeUnit.SECONDS));
  }

  @Test
  public void testToFuture_Error() {
    Exception testError = new RuntimeException("test error");

    CompletableFuture<String> future =
        CallbackAdapter.toFuture(callback -> callback.onError(testError));

    ExecutionException ex =
        assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
    assertEquals(testError, ex.getCause());
  }

  @Test
  public void testToFuture_ExceptionInOperation() {
    RuntimeException testError = new RuntimeException("operation error");

    CompletableFuture<String> future =
        CallbackAdapter.toFuture(
            callback -> {
              throw testError;
            });

    ExecutionException ex =
        assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
    assertEquals(testError, ex.getCause());
  }

  @Test
  public void testFromFuture_Success() throws Exception {
    CompletableFuture<String> future = CompletableFuture.completedFuture("result");
    TestCallback<String> callback = new TestCallback<>();

    CallbackAdapter.fromFuture(future, callback);

    String result = callback.awaitResult();
    assertEquals("result", result);
  }

  @Test
  public void testFromFuture_Error() throws Exception {
    Exception testError = new RuntimeException("error");
    CompletableFuture<String> future = new CompletableFuture<>();
    future.completeExceptionally(testError);

    TestCallback<String> callback = new TestCallback<>();
    CallbackAdapter.fromFuture(future, callback);

    Thread.sleep(100); // Give callback time to execute
    assertTrue(callback.isComplete());
    assertEquals(testError, callback.getError());
  }

  @Test
  public void testRoundTrip_SuccessPath() throws Exception {
    // Callback -> Future -> Callback
    CompletableFuture<String> future =
        CallbackAdapter.toFuture(callback -> callback.onSuccess("roundtrip"));

    TestCallback<String> callback = new TestCallback<>();
    CallbackAdapter.fromFuture(future, callback);

    assertEquals("roundtrip", callback.awaitResult());
  }

  @Test
  public void testRoundTrip_ErrorPath() throws Exception {
    Exception testError = new RuntimeException("roundtrip error");

    // Callback -> Future -> Callback
    CompletableFuture<String> future =
        CallbackAdapter.toFuture(callback -> callback.onError(testError));

    TestCallback<String> callback = new TestCallback<>();
    CallbackAdapter.fromFuture(future, callback);

    Thread.sleep(100); // Give callback time to execute
    assertTrue(callback.isComplete());
    assertEquals(testError, callback.getError());
  }

  @Test
  public void testToFuture_WithAsyncOperation() throws Exception {
    // Simulate an async operation
    CompletableFuture<String> future =
        CallbackAdapter.toFuture(
            callback -> {
              new Thread(
                      () -> {
                        try {
                          Thread.sleep(50);
                          callback.onSuccess("async result");
                        } catch (InterruptedException e) {
                          callback.onError(e);
                        }
                      })
                  .start();
            });

    assertEquals("async result", future.get(2, TimeUnit.SECONDS));
  }

  @Test
  public void testFromFuture_WithAsyncCompletion() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    TestCallback<String> callback = new TestCallback<>();

    CallbackAdapter.fromFuture(future, callback);

    // Complete the future asynchronously
    new Thread(
            () -> {
              try {
                Thread.sleep(50);
                future.complete("delayed result");
              } catch (InterruptedException e) {
                future.completeExceptionally(e);
              }
            })
        .start();

    assertEquals("delayed result", callback.awaitResult(2, TimeUnit.SECONDS));
  }
}

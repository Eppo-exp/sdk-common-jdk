package cloud.eppo;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Publishes public results without lending SDK coordination threads to caller continuations. */
final class ConfigurationResultPublisher {
  private static final int DEFAULT_CAPACITY = 4;
  private static final AtomicInteger THREAD_IDS = new AtomicInteger();

  private final Semaphore slots;
  private final ThreadPoolExecutor executor;

  ConfigurationResultPublisher() {
    this(DEFAULT_CAPACITY, 30, TimeUnit.SECONDS);
  }

  ConfigurationResultPublisher(int capacity, long idleTimeout, TimeUnit unit) {
    slots = new Semaphore(capacity);
    executor =
        new ThreadPoolExecutor(
            capacity,
            capacity,
            idleTimeout,
            unit,
            new ArrayBlockingQueue<>(capacity),
            task -> {
              Thread thread =
                  new Thread(task, "eppo-configuration-result-" + THREAD_IDS.incrementAndGet());
              thread.setDaemon(true);
              return thread;
            });
    // A requestor need not be closed to release its idle publication threads. Threads are only
    // created when a result is ready, and the thread factory does not retain the requestor.
    executor.allowCoreThreadTimeOut(true);
  }

  <T> Publication<T> reserve() {
    return slots.tryAcquire() ? new Publication<>() : null;
  }

  int workerCount() {
    return executor.getPoolSize();
  }

  final class Publication<T> {
    private final CompletableFuture<T> result = new CompletableFuture<>();

    // Only for synchronous setup failures before follow() or public exposure.
    void abandon() {
      slots.release();
    }

    CompletableFuture<T> follow(CompletableFuture<T> operation, boolean cancelOperation) {
      if (cancelOperation) {
        result.whenComplete(
            (ignored, failure) -> {
              if (result.isCancelled()) {
                operation.cancel(true);
              }
            });
      }
      operation.whenComplete(
          (value, failure) ->
              executor.execute(
                  () -> {
                    try {
                      if (operation.isCancelled()) {
                        result.cancel(false);
                      } else if (failure != null) {
                        result.completeExceptionally(failure);
                      } else {
                        result.complete(value);
                      }
                    } finally {
                      // complete() runs already-attached caller continuations inline. Retain this
                      // slot until they return, so recursively joined async loads fail admission
                      // before all publishers become occupied waiting for another publication.
                      slots.release();
                    }
                  }));
      // At most capacity operations can submit one publication each. The private executor is never
      // shut down and has a capacity-sized queue, so an admitted result cannot be rejected or fall
      // back to running caller code on a persistence/coordination thread.
      return result;
    }
  }
}

package cloud.eppo;

import cloud.eppo.api.SerializableEppoConfiguration;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Manages configuration loading and refresh for the Eppo client.
 *
 * <p>Implementations must be thread-safe; multiple threads may call these methods concurrently.
 */
interface ConfigurationRequestor<ConfigurationType extends SerializableEppoConfiguration> {

  /**
   * Asynchronously sets the initial configuration from a future. Returns {@code true} if the
   * configuration was applied, {@code false} if it was empty, failed, or a remote save took
   * precedence. Pending remote saves settle before this decision; an earlier successful remote save
   * keeps precedence even if a later fetch fails. Public completion does not wait for customer
   * callbacks. If all result publishers are occupied, returns {@code false} without registering or
   * applying the initial configuration; a subsequent call may retry registration.
   *
   * @throws IllegalStateException if called more than once
   */
  CompletableFuture<Boolean> setInitialConfiguration(
      @NotNull CompletableFuture<ConfigurationType> configurationFuture);

  /** Fetches and saves the configuration from the remote API synchronously. */
  void fetchAndSaveFromRemote();

  /**
   * Fetches and saves the configuration from the remote API asynchronously. Cancelling the returned
   * future does not cancel a remote configuration already admitted for saving, including one
   * waiting for initial persistence. A flag response still pending when its async fetch is replaced
   * is discarded before admission. Cancellation does not promise to interrupt the underlying I/O.
   *
   * <p>Persistence, precedence and ETag state are settled before the returned future completes
   * successfully. Completion runs on a bounded publisher separate from persistence and
   * coordination. A caller's continuations may block a publisher, but do not delay synchronous
   * loads or internal persistence. At most four asynchronous results may be pending publication or
   * executing attached continuations per requestor. If these slots are occupied, the returned
   * future fails immediately with {@link java.util.concurrent.RejectedExecutionException} before a
   * new remote request starts. In particular, recursively joining asynchronous loads cannot exhaust
   * publishers and deadlock the SDK: the first load exceeding this limit fails admission. Idle
   * publisher threads are released.
   */
  CompletableFuture<Void> fetchAndSaveFromRemoteAsync();
}

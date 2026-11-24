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
   * keeps precedence even if a later fetch fails.
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
   * is discarded before admission.
   */
  CompletableFuture<Void> fetchAndSaveFromRemoteAsync();
}

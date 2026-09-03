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
   * configuration was applied, {@code false} if a remote fetch completed first.
   *
   * @throws IllegalStateException if called more than once
   */
  CompletableFuture<Boolean> setInitialConfiguration(
      @NotNull CompletableFuture<ConfigurationType> configurationFuture);

  /**
   * Fetches and saves the configuration from the remote API synchronously. Cancels any in-progress
   * async fetch before starting.
   */
  void fetchAndSaveFromRemote();

  /**
   * Fetches and saves the configuration from the remote API asynchronously. Cancels any in-progress
   * async fetch before starting.
   */
  CompletableFuture<Void> fetchAndSaveFromRemoteAsync();
}

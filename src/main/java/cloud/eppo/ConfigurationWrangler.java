package cloud.eppo;

import cloud.eppo.api.Configuration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: handle bandit stuff
public class ConfigurationWrangler {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationWrangler.class);

  private final ConfigurationRequestor requestor;
  private final IConfigurationStore configurationStore;

  // This reference is **the** reference to the current configuration.
  private volatile Configuration config;

  public ConfigurationWrangler(
      ConfigurationRequestor requestor,
      IConfigurationStore persistentSource,
      Configuration initialConfiguration) {
    this.requestor = requestor;
    this.configurationStore = persistentSource;

    setConfig(initialConfiguration != null ? initialConfiguration : Configuration.emptyConfig());
  }

  private final ReentrantLock lock = new ReentrantLock();

  private void setConfig(Configuration configuration) {
    lock.lock();
    try {
      config = configuration;

      // Fire-and-forget save call to the config store. It may or may not block (non-blocking is
      // best)
      configurationStore.saveConfiguration(config);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Loads the configuration from the `ConfigurationRequestor` and saves to "persistent" storage
   * using `IConfigurationStore`
   *
   * <p>This method attempts to load from the `IConfigurationStore` if it was unable to load from
   * the API.
   */
  public void load() {
    try {
      Configuration newConfig = requestor.load(config);
      setConfig(newConfig);
    } catch (RuntimeException e) {
      log.error("Failed to load configuration", e);

      // Try getting config from the cache. It loads async, so we'll use a blocking queue.
      BlockingQueue<Configuration> bq = new LinkedBlockingQueue<>(1);

      configurationStore.load(
          new IConfigurationStore.CacheLoadCallback() {
            @Override
            public void onSuccess(Configuration result) {
              bq.add(result);
            }

            @Override
            public void onFailure(String errorMessage) {
              throw new RuntimeException(errorMessage);
            }
          });

      try {
        // Only set the config reference as this value came from the cache
        config = bq.take();
      } catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }

    setConfig(requestor.load(config));
  }

  // Load Async Method
  // Start an async load via the configStore (which can be subclassed by libraries using this
  // package to load from disk/cache/etc)
  // Also start an async http fetch (this is required for Android to have network calls off the main
  // thread, which is a good idea anyway).
  //
  // The precedence strategy is first-to-succeed, which is to say, the first loader to succeed gets
  // to call
  // `callback.onComplete()`.
  //
  // If the cache load succeeds, it writes the config iff the fetch has not yet set a value
  // If the cache load fails, it calls `onFailure` iff the fetch also failed. Only the fetch error
  // message is ever returned.
  //
  // If the fetch succeeds, it writes its config regardless of the cache result
  // The fetch success will trigger the callback only if it has not yet been called.
  // If the fetch fails and the cache failed pass the fetch error message to the callback
  // (`onFailure`)
  public void loadAsync(LoadCallback callback) {
    loadAsync(false, callback);
  }

  public void loadAsync(final boolean skipCache, LoadCallback callback) {
    if (skipCache) {
      loadAsyncFromRequestor(callback);
      return;
    }

    // We have two at-bats to load the configuration: loading from cache and fetching
    // The below variables help them keep track of each other's progress
    AtomicBoolean cacheLoadInProgress = new AtomicBoolean(true);
    AtomicReference<String> fetchErrorMessage = new AtomicReference<>(null);
    // We only want to fire the callback off once; so track whether or not we have yet
    AtomicBoolean callbackCalled = new AtomicBoolean(false);

    configurationStore.load(
        new IConfigurationStore.CacheLoadCallback() {
          @Override
          public void onSuccess(Configuration result) {
            lock.lock();
            try {
              cacheLoadInProgress.set(false);
              // If cache loaded successfully, fire off success callback if not yet done so by
              // fetching
              if (callback != null && callbackCalled.compareAndSet(false, true)) {
                log.debug("Initialized from cache");
                callback.onSuccess(null);
              }
            } finally {
              lock.unlock();
            }
          }

          @Override
          public void onFailure(String errorMessage) {
            cacheLoadInProgress.set(false);
            log.debug("Did not initialize from cache");
            // If cache loading failed, and fetching failed, fire off the failure callback if not
            // yet done so
            // Otherwise, if fetching has not failed yet, defer to it for firing off callbacks
            if (callback != null
                && fetchErrorMessage.get() != null
                && callbackCalled.compareAndSet(false, true)) {
              log.error("Failed to initialize from fetching or by cache");
              callback.onFailure("Cache and fetch failed " + fetchErrorMessage.get());
            }
          }
        });

    log.debug("Fetching configuration");
    requestor.loadAsync(
        config,
        new ConfigurationRequestor.ConfigurationCallback() {

          @Override
          public void onSuccess(Configuration result) {
            lock.lock();
            try {
              log.debug("Configuration fetch successful");
              setConfig(result);
              // If fetching succeeded, fire off success callback if not yet done so from cache
              // loading
              if (callback != null && callbackCalled.compareAndSet(false, true)) {
                log.debug("Initialized from fetch");
                callback.onSuccess(null);
              }
            } catch (Exception e) {
              fetchErrorMessage.set(e.getMessage());
              log.error("Error loading configuration response", e);
              // If fetching failed, and cache loading failed, fire off the failure callback if not
              // yet done so
              // Otherwise, if cache has not finished yet, defer to it for firing off callbacks
              if (callback != null
                  && !cacheLoadInProgress.get()
                  && callbackCalled.compareAndSet(false, true)) {
                log.debug("Failed to initialize from cache or by fetching");
                callback.onFailure("Cache and fetch failed " + e.getMessage());
              }
            } finally {
              lock.unlock();
            }
          }

          @Override
          public void onFailure(String errorMessage) {
            fetchErrorMessage.set(errorMessage);
            log.error("Error fetching configuration: " + errorMessage);
            // If fetching failed, and cache loading failed, fire off the failure callback if not
            // yet done so
            // Otherwise, if cache has not finished yet, defer to it for firing off callbacks
            if (callback != null
                && !cacheLoadInProgress.get()
                && callbackCalled.compareAndSet(false, true)) {
              log.debug("Initialization failure due to fetch error");
              callback.onFailure(errorMessage);
            }
          }
        });
  }

  private void loadAsyncFromRequestor(LoadCallback callback) {
    requestor.loadAsync(
        config,
        new ConfigurationRequestor.ConfigurationCallback() {
          @Override
          public void onSuccess(Configuration result) {
            setConfig(result);
            callback.onSuccess(null);
          }

          @Override
          public void onFailure(String errorMessage) {
            callback.onFailure(errorMessage);
          }
        });
  }

  public Configuration getConfiguration() {
    return config;
  }

  public interface LoadCallback extends EppoCallback<Void> {}
}

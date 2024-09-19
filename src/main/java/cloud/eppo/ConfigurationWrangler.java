package cloud.eppo;

import cloud.eppo.api.Configuration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.Nullable;
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
      // Save to the config store.
      configurationStore.setConfiguration(config);
    } finally {
      lock.unlock();
    }
  }

  // Load Method
  // 1 Enqueue an async load via the configStore (which can be subclassed by libraries using this
  // package to load from disk/cache/etc)
  //    And enqueue an http fetch.
  // 2 Set the first to return as the config
  // 3 Set the second to return as the config iff the second to return is the fetch request.
  // 4 Then, push the config to the config store for optional storage, _however_,
  // `ConfigurationWrangler` remains the
  //     keeper of the source-of-truth reference to the current configuration.

  // TODO: async loading for android
  public void load() {
    BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();
    loadAsync(
        new LoadCallback() {
          @Override
          public void onComplete() {
            queue.add(true);
          }

          @Override
          public void onError(String error) {
            queue.add(false);
          }
        });

    try {
      queue.take();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void loadAsync(LoadCallback callback) {

    // We have two at-bats to load the configuration: loading from cache and fetching
    // The below variables help them keep track of each other's progress
    AtomicReference<String> fetchErrorMessage = new AtomicReference<>(null);
    AtomicReference<String> cacheError = new AtomicReference<>(null);

    // We only want to fire the callback off once; so track whether we have yet
    AtomicBoolean callbackCalled = new AtomicBoolean(false);
    AtomicBoolean authorityReturned = new AtomicBoolean(false);
    // Queue up the cacheload
    configurationStore.load(
        configuration -> {
          synchronized (lock) {
            // If cache loaded successfully, fire off success callback if not yet done so by
            // fetching
            if (!authorityReturned.get()) {
              setConfig(configuration);
            }
            if (callback != null && callbackCalled.compareAndSet(false, true)) {
              log.debug("Initialized from cache");
              callback.onComplete();
            }

            //                storeReturned.set(true);
            //                // Cache store is not authoritative.
            //                if (callback != null  && !authorityReturned.get()) {
            //                  setConfig(configuration);
            //                  callback.onComplete();
            //                }
          }
        },
        error -> {
          synchronized (lock) {
            log.debug("Did not initialize from cache " + error.getMessage());
            // If cache loading failed, and fetching failed, fire off the failure callback if not
            // yet done so
            // Otherwise, if fetching has not failed yet, defer to it for firing off callbacks
            if (fetchErrorMessage.get() != null) {
              log.error("Failed to initialize from fetching and by cache");
            }
            cacheError.set(error.getMessage());

            if (callback != null
                && fetchErrorMessage.get() != null
                && callbackCalled.compareAndSet(false, true)) {
              callback.onError("Cache and fetch failed " + fetchErrorMessage.get());
            }
          }
          //
          //              // If the cache throws an error, we still have the Authoritative source.
          // If both have thrown an error, we
          //              // want to fail
          //              if (fetchError.get() != null) {
          //               callback.onError(fetchError.get());
          //              } else {
          //                cacheError.set(error);
          //              }
        });

    log.debug("Fetching configuration from API");
    // Kick off async fetch load
    requestor.loadAsync(
        config,
        configuration -> {
          // Requestor is getting the very latest data and so is authoritative.
          synchronized (lock) {
            setConfig(configuration);
            authorityReturned.set(true);
            if (callback != null && !callbackCalled.get()) {
              // Complete the callback only if the cacheload hasn't yet returned
              callback.onComplete();
            }
          }
        },
        error -> {
          synchronized (lock) {
            log.error("Error fetching configuration from API: " + error.getMessage());

            fetchErrorMessage.set(error.getMessage());
            // If fetching failed, and cache loading failed, fire off the failure callback if not
            // yet done so
            // Otherwise, if cache has not finished yet, defer to it for firing off callbacks
            if (callback != null
                && cacheError.get() != null
                && callbackCalled.compareAndSet(false, true)) {
              log.debug("Initialization failure due to fetch error");
              callback.onError(error.getMessage());
            }
          }
        });
  }

  // Grab hold of the last configuration in case its bandit models are useful
  //    Configuration lastConfig = config;
  //
  //    log.debug("Fetching configuration");
  //    byte[] flagConfigurationJsonBytes = requestBody("/api/flag-config/v1/config");
  //    Configuration.Builder configBuilder =
  //            new Configuration.Builder(flagConfigurationJsonBytes, expectObfuscatedConfig)
  //                    .banditParametersFromConfig(lastConfig);
  //
  //    if (configBuilder.requiresBanditModels()) {
  //      byte[] banditParametersJsonBytes = requestBody("/api/flag-config/v1/bandits");
  //      configBuilder.banditParameters(banditParametersJsonBytes);
  //    }
  //
  //    config = configBuilder.build();
  //    configurationStore.setConfiguration(config);

  public Configuration getConfiguration() {
    return config;
  }

  public interface LoadCallback {
    void onComplete();

    void onError(String error);
  }
}

class ConfigResult {
  @Nullable public final Configuration configuration;
  public final boolean isAuthoritative;
  @Nullable public final String error;

  public boolean success() {
    return error == null;
  }

  public ConfigResult(
      @Nullable final Configuration configuration,
      boolean isAuthoritative,
      @Nullable String error) {
    this.configuration = configuration;
    this.error = error;
    this.isAuthoritative = isAuthoritative;
  }

  public static ConfigResult success(
      @Nullable Configuration configuration, boolean isAuthoritative) {
    return new ConfigResult(configuration, isAuthoritative, null);
  }

  public static ConfigResult error(@Nullable String error) {
    return new ConfigResult(null, false, error);
  }
}

//
//
//        //    Configuration config = requestor.load(this.config);
////    setConfig(config);
//        BlockingQueue<ConfigResult> queue = new LinkedBlockingQueue<>(2);
//
//        // Queue up the cacheload
//    configurationStore.load(
//        configuration -> {
//        // Cache store is not authoritative.
//        queue.add(ConfigResult.success(configuration, false));
//        },
//        error -> {
//        queue.add(ConfigResult.error(error.getMessage()));
//        }
//        );
//
//        // Kick off async fetch load
//        requestor.loadAsync(
//        config,
//        configuration -> {
//        // Requestor is getting the very latest data and so is authoritative.
//        queue.add(ConfigResult.success(configuration, true));
//        },
//        error -> {
//        queue.add(ConfigResult.error(error.getMessage()));
//        }
//        );
//
//        // take and apply the first config to return
//        try {
//        ConfigResult first = queue.take();
//      if (first.success()) {
//        setConfig(first.configuration);
//      } else {
//              throw new RuntimeException(first.error);
//      }
//              } catch (InterruptedException e) {
//        throw new RuntimeException(e);
//    }
//
//            // Take the second and only apply it if it is authoritative
//            try {
//        ConfigResult second = queue.take();
//      if (second.success() && second.isAuthoritative) {
//        setConfig(second.configuration);
//      } else if (!second.success()) {
//        throw new RuntimeException(second.error);
//      }
//              } catch (InterruptedException e) {
//        throw new RuntimeException(e);
//    }

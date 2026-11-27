package cloud.eppo;

import cloud.eppo.api.SerializableEppoConfiguration;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Keeps public configuration observers outside the store's persistence and notification work. */
final class ConfigurationObserverDispatcher<
    ConfigurationType extends SerializableEppoConfiguration> {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationObserverDispatcher.class);
  private static final AtomicLong nextDispatcherId = new AtomicLong();
  private static final int MAX_PENDING_NOTIFICATIONS = 256;
  private static final long IDLE_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30);

  private final IConfigurationStore<ConfigurationType> store;
  private final Executor executor;
  // Identity is part of the public unsubscribe-by-reference contract.
  private final Map<Consumer<ConfigurationType>, List<Registration<ConfigurationType>>>
      registrations = new IdentityHashMap<>();

  ConfigurationObserverDispatcher(IConfigurationStore<ConfigurationType> store) {
    this(store, createExecutor(MAX_PENDING_NOTIFICATIONS, IDLE_TIMEOUT_MILLIS));
  }

  ConfigurationObserverDispatcher(IConfigurationStore<ConfigurationType> store, Executor executor) {
    this.store = Objects.requireNonNull(store, "store");
    this.executor = Objects.requireNonNull(executor, "executor");
  }

  static ThreadPoolExecutor createExecutor(int capacity, long idleTimeoutMillis) {
    String name = "eppo-configuration-observer-" + nextDispatcherId.incrementAndGet();
    return new ThreadPoolExecutor(
        0,
        1,
        idleTimeoutMillis,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(capacity),
        task -> {
          Thread thread = new Thread(task, name);
          thread.setDaemon(true);
          return thread;
        },
        new ThreadPoolExecutor.AbortPolicy());
  }

  Runnable subscribe(Consumer<ConfigurationType> callback) {
    Objects.requireNonNull(callback, "callback");
    Registration<ConfigurationType> registration = new Registration<>(this, callback);
    synchronized (registrations) {
      registrations.computeIfAbsent(callback, ignored -> new ArrayList<>()).add(registration);
    }
    try {
      registration.installUnsubscribe(store.subscribe(registration::enqueue));
    } catch (RuntimeException | Error failure) {
      removeRegistration(callback, registration);
      registration.deactivate();
      throw failure;
    }
    return registration;
  }

  boolean unsubscribe(Consumer<ConfigurationType> callback) {
    List<Registration<ConfigurationType>> removed;
    synchronized (registrations) {
      removed = registrations.remove(callback);
    }
    if (removed == null) {
      return false;
    }
    RuntimeException firstFailure = null;
    for (Registration<ConfigurationType> registration : removed) {
      try {
        registration.deactivate();
      } catch (RuntimeException failure) {
        if (firstFailure == null) {
          firstFailure = failure;
        } else {
          firstFailure.addSuppressed(failure);
        }
      }
    }
    if (firstFailure != null) {
      throw firstFailure;
    }
    return true;
  }

  private void unsubscribe(
      Consumer<ConfigurationType> callback, Registration<ConfigurationType> registration) {
    removeRegistration(callback, registration);
    registration.deactivate();
  }

  private void removeRegistration(
      Consumer<ConfigurationType> callback, Registration<ConfigurationType> registration) {
    synchronized (registrations) {
      List<Registration<ConfigurationType>> existing = registrations.get(callback);
      if (existing != null) {
        existing.remove(registration);
        if (existing.isEmpty()) {
          registrations.remove(callback);
        }
      }
    }
  }

  private static final class Registration<ConfigurationType extends SerializableEppoConfiguration>
      implements Runnable {
    private ConfigurationObserverDispatcher<ConfigurationType> owner;
    private Consumer<ConfigurationType> callback;
    private Runnable unsubscribe;
    private boolean active = true;

    private Registration(
        ConfigurationObserverDispatcher<ConfigurationType> owner,
        Consumer<ConfigurationType> callback) {
      this.owner = owner;
      this.callback = callback;
    }

    @Override
    public void run() {
      ConfigurationObserverDispatcher<ConfigurationType> dispatcher;
      Consumer<ConfigurationType> observer;
      synchronized (this) {
        if (!active) {
          return;
        }
        dispatcher = owner;
        observer = callback;
      }
      dispatcher.unsubscribe(observer, this);
    }

    private void installUnsubscribe(Runnable unsubscribe) {
      Objects.requireNonNull(unsubscribe, "unsubscribe");
      boolean alreadyRemoved;
      synchronized (this) {
        alreadyRemoved = !active;
        if (!alreadyRemoved) {
          this.unsubscribe = unsubscribe;
        }
      }
      // A custom store may notify during subscribe, so removal can race with registration.
      if (alreadyRemoved) {
        unsubscribe.run();
      }
    }

    private void deactivate() {
      Runnable remove;
      synchronized (this) {
        if (!active) {
          return;
        }
        active = false;
        owner = null;
        callback = null;
        remove = unsubscribe;
        unsubscribe = null;
      }
      if (remove != null) {
        remove.run();
      }
    }

    private void enqueue(ConfigurationType configuration) {
      Executor deliveryExecutor;
      synchronized (this) {
        if (!active) {
          return;
        }
        deliveryExecutor = owner.executor;
      }
      try {
        deliveryExecutor.execute(() -> deliver(configuration));
      } catch (RejectedExecutionException failure) {
        // Delivery failure must not change the result of an already-successful persistence.
        log.warn(
            "Configuration observer notification rejected; configuration remains active", failure);
      }
    }

    private void deliver(ConfigurationType configuration) {
      Consumer<ConfigurationType> observer;
      synchronized (this) {
        if (!active) {
          return;
        }
        observer = callback;
      }
      // A delivery selected before unsubscription is in flight and may finish. Never invoke
      // customer code while holding the registration or store's notification monitor.
      try {
        observer.accept(configuration);
      } catch (Exception failure) {
        log.error("Configuration observer failed", failure);
      }
    }
  }
}

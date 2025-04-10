package cloud.eppo;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import cloud.eppo.api.Configuration;
import cloud.eppo.callback.CallbackManager;

public class ConfigurationRequestorJava6 {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestorJava6.class);
  private enum ConfigState {
    UNSET,
    INITIAL_SET,
    REMOTE_SET,
    ;
  }

  private final EppoHttpClient client;
  private final IConfigurationStoreJava6 configurationStoreJava6;
  private final boolean expectObfuscatedConfig;
  private final boolean supportBandits;

  private final Object configStateLock = new Object();
  private ConfigState configState;

  private final CallbackManager<Configuration> configChangeManager = new CallbackManager<>();

  public ConfigurationRequestorJava6(
      @NotNull IConfigurationStoreJava6 configurationStoreJava6,
      @NotNull EppoHttpClient client,
      boolean expectObfuscatedConfig,
      boolean supportBandits) {
    this.configurationStoreJava6 = configurationStoreJava6;
    this.client = client;
    this.expectObfuscatedConfig = expectObfuscatedConfig;
    this.supportBandits = supportBandits;

    synchronized (configStateLock) {
      configState = ConfigState.UNSET;
    }
  }

  // Synchronously set the initial configuration.
  public boolean setInitialConfigurationJava6(@NotNull Configuration configuration) {
    synchronized (configStateLock) {
      switch(configState) {
        case UNSET:
          configState = ConfigState.INITIAL_SET;
          break;
        case INITIAL_SET:
          throw new IllegalStateException("Initial configuration has already been set");
        case REMOTE_SET:
          return false;
      }
    }

    saveConfigurationAndNotifyJava6(configuration);
    return true;
  }

  /** Loads configuration synchronously from the API server. */
  void fetchAndSaveFromRemoteJava6() {
    log.debug("Fetching configuration");

    // Reuse the `lastConfig` as its bandits may be useful
    Configuration lastConfig = configurationStoreJava6.getConfiguration();

    byte[] flagConfigurationJsonBytes = client.getJava6(Constants.FLAG_CONFIG_ENDPOINT);
    Configuration.Builder configBuilder =
        Configuration.builder(flagConfigurationJsonBytes, expectObfuscatedConfig)
            .banditParametersFromConfig(lastConfig);

    if (supportBandits && configBuilder.requiresUpdatedBanditModels()) {
      byte[] banditParametersJsonBytes = client.getJava6(Constants.BANDIT_ENDPOINT);
      configBuilder.banditParameters(banditParametersJsonBytes);
    }

    synchronized (configStateLock) {
      configState = ConfigState.REMOTE_SET;
    }
    saveConfigurationAndNotifyJava6(configBuilder.build());
  }

  private void saveConfigurationAndNotifyJava6(Configuration configuration) {
    configurationStoreJava6.saveConfigurationJava6(configuration);
    synchronized (configChangeManager) {
      configChangeManager.notifyCallbacks(configuration);
    }
  }

  public Runnable onConfigurationChange(Consumer<Configuration> callback) {
    return configChangeManager.subscribe(callback);
  }
}

package cloud.eppo;

import cloud.eppo.api.Configuration;
import cloud.eppo.configuration.IConfigurationSource;
import org.jetbrains.annotations.NotNull;

/** In memory store of a configuration snapshot. */
public class ConfigurationStore implements IConfigurationSource, IConfigurationStore {

  private volatile Configuration configuration = null;

  public ConfigurationStore() {}

  @Override
  public void setConfiguration(@NotNull final Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void load(SuccessCallback successCallback, FailureCallback failureCallback) {
    // Immediately return the configuration
    successCallback.onSuccess(configuration);
  }
}

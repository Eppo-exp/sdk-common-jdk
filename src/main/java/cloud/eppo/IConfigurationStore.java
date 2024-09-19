package cloud.eppo;

import cloud.eppo.api.Configuration;
import cloud.eppo.configuration.IConfigurationSource;
import org.jetbrains.annotations.NotNull;

public interface IConfigurationStore extends IConfigurationSource {
  public void setConfiguration(@NotNull final Configuration configuration);
}

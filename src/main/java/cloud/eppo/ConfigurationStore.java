package cloud.eppo;

import cloud.eppo.configuration.ConfigurationBuffer;
import cloud.eppo.ufc.dto.*;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationStore {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationStore.class);

  private ConfigurationBuffer configurationBuffer;

  public ConfigurationStore(final ConfigurationBuffer initialConfiguration) {
    if (initialConfiguration != null) {
      this.configurationBuffer = initialConfiguration;
    } else {
      configurationBuffer = new ConfigurationBuffer();
    }
  }

  public void setConfiguration(@NotNull final ConfigurationBuffer configuration) {
    configurationBuffer = configuration;
  }

  public FlagConfig getFlag(String flagKey) {
    Map<String, FlagConfig> flags = configurationBuffer.getFlags();
    if (flags == null) {
      log.warn("Request for flag {} before flags have been loaded", flagKey);
      return null;
    } else if (flags.isEmpty()) {
      log.warn("Request for flag {} with empty flags", flagKey);
    }
    return flags.get(flagKey);
  }

  public String banditKeyForVariation(String flagKey, String variationValue) {
    // Note: In practice this double loop should be quite quick as the number of bandits and bandit
    // variations will be small. Should this ever change, we can optimize things.
    for (Map.Entry<String, BanditReference> banditEntry :
        configurationBuffer.getBanditReferences().entrySet()) {
      BanditReference banditReference = banditEntry.getValue();
      for (BanditFlagVariation banditFlagVariation : banditReference.getFlagVariations()) {
        if (banditFlagVariation.getFlagKey().equals(flagKey)
            && banditFlagVariation.getVariationValue().equals(variationValue)) {
          return banditEntry.getKey();
        }
      }
    }
    return null;
  }

  public BanditParameters getBanditParameters(String banditKey) {
    return configurationBuffer.getBandits().get(banditKey);
  }
}

package cloud.eppo.configuration;

import cloud.eppo.ufc.dto.*;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper object passed between the various Configuration handling classes encapsulating flag and
 * bandit configuration as well as whether the config is obfuscated.
 */
public class ConfigurationBuffer {

  private static final Logger log = LoggerFactory.getLogger(ConfigurationBuffer.class);

  private final Map<String, BanditReference> banditReferences;
  private final Map<String, FlagConfig> flags;
  private Map<String, BanditParameters> bandits;

  private final ObjectMapper mapper = new ObjectMapper().registerModule(EppoModule.eppoModule());

  private final boolean isConfigObfuscated;

  /** Creates an empty configuration buffer. */
  public ConfigurationBuffer() {
    flags = Collections.emptyMap();
    banditReferences = Collections.emptyMap();
    bandits = Collections.emptyMap();
    isConfigObfuscated = false;
  }

  public ConfigurationBuffer(String flagJson) {
    this(flagJson, false);
  }

  public ConfigurationBuffer(String flagJson, String banditJson) {
    this(flagJson, banditJson, false);
  }

  public ConfigurationBuffer(String flagJson, String banditJson, boolean isConfigObfuscated) {
    this(flagJson, isConfigObfuscated);
    setBandits(banditJson);
  }

  public ConfigurationBuffer(String flagJson, boolean isConfigObfuscated) {
    this.isConfigObfuscated = isConfigObfuscated;
    bandits = Collections.emptyMap();
    if (flagJson == null) {
      flags = Collections.emptyMap();
      banditReferences = Collections.emptyMap();
    } else {
      // Build the flags config from the json string.
      FlagConfigResponse config;
      try {
        config = mapper.readValue(flagJson, FlagConfigResponse.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }

      flags = Collections.unmodifiableMap(config.getFlags());
      banditReferences = Collections.unmodifiableMap(config.getBanditReferences());
    }
  }

  public void setBandits(String banditParameterJson) {
    BanditParametersResponse config;
    try {
      config = mapper.readValue(banditParameterJson, BanditParametersResponse.class);
    } catch (JsonProcessingException e) {
      log.error("Unable to parse bandit parameters response");
      throw new RuntimeException(e);
    }

    if (config == null || config.getBandits() == null) {
      log.warn("Bandits missing in bandit parameters response");
      bandits = Collections.emptyMap();
    } else {
      bandits = Collections.unmodifiableMap(config.getBandits());
      log.debug("Loaded {} bandit models from configuration response", bandits.size());
    }
  }

  public Map<String, FlagConfig> getFlags() {
    return flags;
  }

  public Map<String, BanditReference> getBanditReferences() {
    return banditReferences;
  }

  public Map<String, BanditParameters> getBandits() {
    return bandits;
  }

  public boolean isConfigObfuscated() {
    return isConfigObfuscated;
  }

  public Set<String> loadedBanditModelVersions() {
    return bandits.values().stream()
        .map(BanditParameters::getModelVersion)
        .collect(Collectors.toSet());
  }

  public Set<String> referencedBanditModelVersion() {
    return banditReferences.values().stream()
        .map(BanditReference::getModelVersion)
        .collect(Collectors.toSet());
  }
}

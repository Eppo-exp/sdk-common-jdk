package cloud.eppo;

import cloud.eppo.ufc.dto.*;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationStore {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationStore.class);
  private final ObjectMapper mapper = new ObjectMapper().registerModule(EppoModule.eppoModule());

  private Map<String, FlagConfig> flags;
  private Map<String, BanditReference> banditReferences;
  private Map<String, BanditParameters> banditParameters;

  public ConfigurationStore() {
    flags = new ConcurrentHashMap<>();
    banditReferences = new ConcurrentHashMap<>();
    banditParameters = new ConcurrentHashMap<>();
  }

  public void setFlagsFromJsonString(String jsonString) {
    FlagConfigResponse config;

    try {
      config = mapper.readValue(jsonString, FlagConfigResponse.class);
    } catch (JsonProcessingException e) {
      log.error("Unable to parse flag configuration response");
      throw new RuntimeException(e);
    }

    if (config == null || config.getFlags() == null) {
      log.warn("Flags missing in configuration response");
      flags = new ConcurrentHashMap<>();
      banditReferences = new ConcurrentHashMap<>();
    } else {
      // TODO: atomic flags to prevent clobbering like android does
      // Record that flags were set from a response so we don't later clobber them with a
      // slow cache read
      flags = new ConcurrentHashMap<>(config.getFlags());
      banditReferences = new ConcurrentHashMap<>(config.getBanditReferences());
      log.debug("Loaded {} flags from configuration response", flags.size());
    }
  }

  public FlagConfig getFlag(String flagKey) {
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
    // variations will be small
    //       Should this ever change, we can optimize things.
    for (Map.Entry<String, BanditReference> banditEntry : banditReferences.entrySet()) {
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

  public Set<String> banditModelVersions() {
    return banditReferences.values().stream()
        .map(BanditReference::getModelVersion)
        .collect(Collectors.toSet());
  }

  public void setBanditParametersFromJsonString(String jsonString) {
    BanditParametersResponse config;

    try {
      config = mapper.readValue(jsonString, BanditParametersResponse.class);
    } catch (JsonProcessingException e) {
      log.error("Unable to parse bandit parameters response");
      throw new RuntimeException(e);
    }

    if (config == null || config.getBandits() == null) {
      log.warn("Bandit missing in bandit parameters response");
      banditParameters = new ConcurrentHashMap<>();
    } else {
      banditParameters = new ConcurrentHashMap<>(config.getBandits());
      log.debug("Loaded {} bandit models from configuration response", banditParameters.size());
    }
  }

  public BanditParameters getBanditParameters(String banditKey) {
    return banditParameters.get(banditKey);
  }
}

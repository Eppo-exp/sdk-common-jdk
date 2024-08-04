package cloud.eppo;

import cloud.eppo.ufc.dto.*;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

  public void setFlagsFromJsonString(String jsonString) throws JsonProcessingException {
    FlagConfigResponse config = mapper.readValue(jsonString, FlagConfigResponse.class);
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
      log.debug("Loaded " + flags.size() + " flags from configuration response");
    }
  }

  public FlagConfig getFlag(String flagKey) {
    if (flags == null) {
      log.warn("Request for flag " + flagKey + " before flags have been loaded");
      return null;
    } else if (flags.isEmpty()) {
      log.warn("Request for flag " + flagKey + " with empty flags");
    }
    return flags.get(flagKey);
  }

  public String banditKeyForVariation(String flagKey, String variationValue) {
    // Note: In practice this double loop should be quite quick as the number of bandits and bandit variations will be small
    //       Should this ever change, we can optimize things.
    for (Map.Entry<String, BanditReference> banditEntry : banditReferences.entrySet()) {
      BanditReference banditReference = banditEntry.getValue();
      for (BanditFlagVariation banditFlagVariation : banditReference.getFlagVariations()) {
        if (banditFlagVariation.getFlagKey().equals(flagKey) && banditFlagVariation.getVariationValue().equals(variationValue)) {
          return banditEntry.getKey();
        }
      }
    }
    return null;
  }

  public BanditParameters getBanditParameters(String banditKey) {
    return banditParameters.get(banditKey);
  }
}

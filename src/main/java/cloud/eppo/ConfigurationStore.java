package cloud.eppo;

import cloud.eppo.ufc.dto.BanditParameters;
import cloud.eppo.ufc.dto.FlagConfig;
import cloud.eppo.ufc.dto.FlagConfigResponse;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationStore {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationStore.class);
  private final ObjectMapper mapper = new ObjectMapper().registerModule(EppoModule.eppoModule());

  private ConcurrentHashMap<String, FlagConfig> flags;
  private ConcurrentHashMap<String, BanditParameters> banditParameters; // TODO: bandit stuff
  // TODO: another map of bandit flag variations to handle no action case
  static ConfigurationStore instance = null;

  public ConfigurationStore() {
    // TODO: handle caching
  }

  public void setFlagsFromJsonString(String jsonString) throws JsonProcessingException {
    FlagConfigResponse config = mapper.readValue(jsonString, FlagConfigResponse.class);
    if (config == null || config.getFlags() == null) {
      log.warn("Flags missing in configuration response");
      flags = new ConcurrentHashMap<>();
    } else {
      // TODO: atomic flags to prevent clobbering like android does
      // Record that flags were set from a response so we don't later clobber them with a
      // slow cache read
      flags = new ConcurrentHashMap<>(config.getFlags());
      log.debug("Loaded " + flags.size() + " flags from configuration response");
    }
  }

  public BanditParameters getBanditParameters(String banditKey) {
    return this.banditParameters.get(banditKey);
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
}
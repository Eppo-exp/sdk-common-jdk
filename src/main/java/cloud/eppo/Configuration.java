package cloud.eppo;

import static cloud.eppo.Utils.getMD5Hex;

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
 * Encapsulates the Flag Configuration and Bandit parameters in an immutable object with a complete
 * and coherent state.
 *
 * <p>A Builder is used to prepare and then create am immutable data structure containing both flag
 * and bandit configurations. An intermediate step is required in building the configuration to
 * accommodate the as-needed loading of bandit parameters as a network call may not be needed if
 * there are no bandits referenced by the flag configuration.
 *
 * <p>Usage: Building with just flag configuration (unobfuscated is default) <code>
 *     Configuration config = new Configuration.Builder(flagConfigJsonString).build();
 * </code>
 *
 * <p>Building with bandits (known configuration) <code>
 *     Configuration config = new Configuration.Builder(flagConfigJsonString).banditParameters(banditConfigJson).build();
 *     </code>
 *
 * <p>Conditionally loading bandit models (with or without an existing bandit config JSON string).
 * <code>
 *  Configuration.Builder configBuilder = new Configuration.Builder(flagConfigJsonString).banditParameters(banditConfigJson);
 *  if (configBuilder.requiresBanditModels()) {
 *    // Load the bandit parameters encoded in a JSON string
 *    configBuilder.banditParameters(banditParameterJsonString);
 *  }
 *  Configuration config = configBuilder.build();
 * </code>
 *
 * <p>
 *
 * <p>Hint: when loading new Flag configuration values, set the current bandit models in the builder
 * then check `requiresBanditModels()`.
 */
public class Configuration {
  private static final Logger log = LoggerFactory.getLogger(Configuration.class);
  private final Map<String, BanditReference> banditReferences;
  private final Map<String, FlagConfig> flags;
  private final Map<String, BanditParameters> bandits;
  private final boolean isConfigObfuscated;

  private Configuration(
      Map<String, FlagConfig> flags,
      Map<String, BanditReference> banditReferences,
      Map<String, BanditParameters> bandits,
      boolean isConfigObfuscated) {
    this.flags = flags;
    this.banditReferences = banditReferences;
    this.bandits = bandits;
    this.isConfigObfuscated = isConfigObfuscated;
  }

  public FlagConfig getFlag(String flagKey) {
    String flagKeyForLookup = flagKey;
    if (isConfigObfuscated()) {
      flagKeyForLookup = getMD5Hex(flagKey);
    }

    if (flags == null) {
      log.warn("Request for flag {} before flags have been loaded", flagKey);
      return null;
    } else if (flags.isEmpty()) {
      log.warn("Request for flag {} with empty flags", flagKey);
    }
    return flags.get(flagKeyForLookup);
  }

  public String banditKeyForVariation(String flagKey, String variationValue) {
    // Note: In practice this double loop should be quite quick as the number of bandits and bandit
    // variations will be small. Should this ever change, we can optimize things.
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

  public BanditParameters getBanditParameters(String banditKey) {
    return bandits.get(banditKey);
  }

  public boolean isConfigObfuscated() {
    return isConfigObfuscated;
  }

  /**
   * Builder to create the immutable config object.
   *
   * @see cloud.eppo.Configuration for usage.
   */
  public static class Builder {
    private static final ObjectMapper mapper =
        new ObjectMapper().registerModule(EppoModule.eppoModule());

    private final boolean isConfigObfuscated;
    private final Map<String, FlagConfig> flags;
    private Map<String, BanditReference> banditReferences;
    private Map<String, BanditParameters> bandits = Collections.emptyMap();

    public static Configuration emptyConfig() {
      return new Configuration(
          Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
    }

    public Builder(String flagJson) {
      this(flagJson, false);
    }

    public Builder(String flagJson, boolean isConfigObfuscated) {
      this.isConfigObfuscated = isConfigObfuscated;

      if (flagJson == null || flagJson.isEmpty()) {
        throw new RuntimeException(
            "Null or empty configuration string. Call `Configuration.Builder.Empty()` instead");
      }

      // Build the flags config from the json string.
      FlagConfigResponse config;
      try {
        config = mapper.readValue(flagJson, FlagConfigResponse.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }

      if (config == null || config.getFlags() == null) {
        log.warn("Flags missing in ufc response");
        flags = Collections.emptyMap();
      } else {
        flags = Collections.unmodifiableMap(config.getFlags());
        banditReferences = Collections.unmodifiableMap(config.getBanditReferences());
        log.debug("Loaded {} flag configs from configuration response", flags.size());
      }
    }

    public boolean requiresBanditModels() {
      Set<String> neededModelVersions = referencedBanditModelVersion();
      return !loadedBanditModelVersions().containsAll(neededModelVersions);
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

    public Builder banditParametersFrom(Configuration currentConfig) {
      if (currentConfig == null || currentConfig.bandits == null) {
        bandits = Collections.emptyMap();
      } else {
        bandits = currentConfig.bandits;
      }
      return this;
    }

    public Builder banditParameters(String banditParameterJson) {
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

      return this;
    }

    public Configuration build() {
      return new Configuration(flags, banditReferences, bandits, isConfigObfuscated);
    }
  }
}

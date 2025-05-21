package cloud.eppo.api;

import static cloud.eppo.Utils.getMD5Hex;

import cloud.eppo.ufc.dto.*;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
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
  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(EppoModule.eppoModule());

  private static final byte[] emptyFlagsBytes =
      "{ \"flags\": {}, \"format\": \"SERVER\" }".getBytes();

  private static final Logger log = LoggerFactory.getLogger(Configuration.class);
  private final Map<String, BanditReference> banditReferences;
  private final Map<String, FlagConfig> flags;
  private final Map<String, BanditParameters> bandits;
  private final boolean isConfigObfuscated;

  @SuppressWarnings("unused")
  private final byte[] flagConfigJson;

  private final byte[] banditParamsJson;

  /** Default visibility for tests. */
  Configuration(
      Map<String, FlagConfig> flags,
      Map<String, BanditReference> banditReferences,
      Map<String, BanditParameters> bandits,
      boolean isConfigObfuscated,
      byte[] flagConfigJson,
      byte[] banditParamsJson) {
    this.flags = flags;
    this.banditReferences = banditReferences;
    this.bandits = bandits;
    this.isConfigObfuscated = isConfigObfuscated;

    // Graft the `forServer` boolean into the flagConfigJson'
    if (flagConfigJson != null && flagConfigJson.length != 0) {
      try {
        JsonNode jNode = mapper.readTree(flagConfigJson);
        FlagConfigResponse.Format format =
            isConfigObfuscated
                ? FlagConfigResponse.Format.CLIENT
                : FlagConfigResponse.Format.SERVER;
        ((ObjectNode) jNode).put("format", format.toString());
        flagConfigJson = mapper.writeValueAsBytes(jNode);
      } catch (IOException e) {
        log.error("Error adding `format` field to FlagConfigResponse JSON");
      }
    }
    this.flagConfigJson = flagConfigJson;
    this.banditParamsJson = banditParamsJson;
  }

  public static Configuration emptyConfig() {
    return new Configuration(
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        false,
        emptyFlagsBytes,
        null);
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

  /**
   * Returns the Variation Type for the specified flag if it exists, otherwise returns null.
   *
   * @return The flag's variation type or null.
   */
  public @Nullable VariationType getFlagType(String flagKey) {
    FlagConfig flag = getFlag(flagKey);
    if (flag == null) {
      return null;
    }
    return flag.getVariationType();
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

  public byte[] serializeFlagConfigToBytes() {
    return flagConfigJson;
  }

  public byte[] serializeBanditParamsToBytes() {
    return banditParamsJson;
  }

  public boolean isEmpty() {
    return flags == null || flags.isEmpty();
  }

  public Set<String> getFlagKeys() {
    if (flags == null) {
      return Collections.emptySet();
    } else {
      return flags.keySet();
    }
  }

  public static Builder builder(byte[] flagJson) {
    return new Builder(flagJson);
  }

  @Deprecated // isConfigObfuscated is determined from the byte payload
  public static Builder builder(byte[] flagJson, boolean isConfigObfuscated) {
    return new Builder(flagJson, isConfigObfuscated);
  }
  /**
   * Builder to create the immutable config object.
   *
   * @see Configuration for usage.
   */
  public static class Builder {

    private final boolean isConfigObfuscated;
    private final Map<String, FlagConfig> flags;
    private final Map<String, BanditReference> banditReferences;
    private Map<String, BanditParameters> bandits = Collections.emptyMap();
    private final byte[] flagJson;
    private byte[] banditParamsJson;

    private static FlagConfigResponse parseFlagResponse(byte[] flagJson) {
      if (flagJson == null || flagJson.length == 0) {
        log.warn("Null or empty configuration string. Call `Configuration.Empty()` instead");
        return null;
      }
      FlagConfigResponse config;
      try {
        return mapper.readValue(flagJson, FlagConfigResponse.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Deprecated // isConfigObfuscated is determined from the byte payload
    public Builder(String flagJson, boolean isConfigObfuscated) {
      this(flagJson.getBytes(), parseFlagResponse(flagJson.getBytes()), isConfigObfuscated);
    }

    @Deprecated // isConfigObfuscated is determined from the byte payload
    public Builder(byte[] flagJson, boolean isConfigObfuscated) {
      this(flagJson, parseFlagResponse(flagJson), isConfigObfuscated);
    }

    public Builder(byte[] flagJson, FlagConfigResponse flagConfigResponse) {
      this(
          flagJson,
          flagConfigResponse,
          flagConfigResponse.getFormat() == FlagConfigResponse.Format.CLIENT);
    }

    /** Use this constructor when the FlagConfigResponse has the `forServer` field populated. */
    public Builder(byte[] flagJson) {
      this(flagJson, parseFlagResponse(flagJson));
    }

    public Builder(
        byte[] flagJson,
        @Nullable FlagConfigResponse flagConfigResponse,
        boolean isConfigObfuscated) {
      this.isConfigObfuscated = isConfigObfuscated;
      this.flagJson = flagJson;
      if (flagConfigResponse == null
          || flagConfigResponse.getFlags() == null
          || flagConfigResponse.getFlags().isEmpty()) {
        log.warn("'flags' map missing in flag definition JSON");
        flags = Collections.emptyMap();
        banditReferences = Collections.emptyMap();
      } else {
        flags = Collections.unmodifiableMap(flagConfigResponse.getFlags());
        banditReferences = Collections.unmodifiableMap(flagConfigResponse.getBanditReferences());
        log.debug("Loaded {} flag definitions from flag definition JSON", flags.size());
      }
    }

    public boolean requiresUpdatedBanditModels() {
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

    public Builder banditParametersFromConfig(Configuration currentConfig) {
      if (currentConfig == null || currentConfig.bandits == null) {
        bandits = Collections.emptyMap();
      } else {
        bandits = currentConfig.bandits;
        banditParamsJson = currentConfig.banditParamsJson;
      }
      return this;
    }

    public Builder banditParameters(String banditParameterJson) {
      return banditParameters(banditParameterJson.getBytes());
    }

    public Builder banditParameters(byte[] banditParameterJson) {
      if (banditParameterJson == null || banditParameterJson.length == 0) {
        log.debug("Bandit parameters are null or empty");
        return this;
      }
      BanditParametersResponse config;
      try {
        config = mapper.readValue(banditParameterJson, BanditParametersResponse.class);
      } catch (IOException e) {
        log.error("Unable to parse bandit parameters JSON");
        throw new RuntimeException(e);
      }

      if (config == null || config.getBandits() == null) {
        log.warn("`bandits` map missing in bandit parameters JSON");
        bandits = Collections.emptyMap();
      } else {
        bandits = Collections.unmodifiableMap(config.getBandits());
        log.debug("Loaded {} bandit models from bandit parameters JSON", bandits.size());
      }

      return this;
    }

    public Configuration build() {
      return new Configuration(
          flags, banditReferences, bandits, isConfigObfuscated, flagJson, banditParamsJson);
    }
  }
}

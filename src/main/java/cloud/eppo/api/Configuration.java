package cloud.eppo.api;

import static cloud.eppo.Utils.getMD5Hex;

import cloud.eppo.ufc.dto.*;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
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
  private final Map<String, ? extends IBanditReference> banditReferences;
  private final Map<String, ? extends IFlagConfig> flags;
  private final Map<String, ? extends IBanditParameters> bandits;
  private final boolean isConfigObfuscated;
  private final String environmentName;
  private final Date configFetchedAt;
  private final Date configPublishedAt;
  private final String flagsETag;

  private final byte[] flagConfigJson;

  private final byte[] banditParamsJson;

  /** Default visibility for tests. */
  Configuration(
      Map<String, ? extends IFlagConfig> flags,
      Map<String, ? extends IBanditReference> banditReferences,
      Map<String, ? extends IBanditParameters> bandits,
      boolean isConfigObfuscated,
      String environmentName,
      Date configFetchedAt,
      Date configPublishedAt,
      String flagsETag,
      byte[] flagConfigJson,
      byte[] banditParamsJson) {
    this.flags = flags;
    this.banditReferences = banditReferences;
    this.bandits = bandits;
    this.isConfigObfuscated = isConfigObfuscated;
    this.environmentName = environmentName;
    this.configFetchedAt = configFetchedAt;
    this.configPublishedAt = configPublishedAt;
    this.flagsETag = flagsETag;

    // Graft the `format` field into the flagConfigJson'
    if (flagConfigJson != null && flagConfigJson.length != 0) {
      try {
        JsonNode jNode = mapper.readTree(flagConfigJson);
        IFlagConfigResponse.Format format =
            isConfigObfuscated
                ? IFlagConfigResponse.Format.CLIENT
                : IFlagConfigResponse.Format.SERVER;
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
        null,
        null,
        null,
        null,
        emptyFlagsBytes,
        null);
  }

  @Override
  public String toString() {
    return "Configuration{"
        + "banditReferences="
        + banditReferences
        + ", flags="
        + flags
        + ", bandits="
        + bandits
        + ", isConfigObfuscated="
        + isConfigObfuscated
        + ", environmentName='"
        + environmentName
        + '\''
        + ", configFetchedAt="
        + configFetchedAt
        + ", configPublishedAt="
        + configPublishedAt
        + ", flagConfigJson="
        + Arrays.toString(flagConfigJson)
        + ", banditParamsJson="
        + Arrays.toString(banditParamsJson)
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Configuration that = (Configuration) o;
    return isConfigObfuscated == that.isConfigObfuscated
        && Objects.equals(banditReferences, that.banditReferences)
        && Objects.equals(flags, that.flags)
        && Objects.equals(bandits, that.bandits)
        && Objects.equals(environmentName, that.environmentName)
        && Objects.equals(configFetchedAt, that.configFetchedAt)
        && Objects.equals(configPublishedAt, that.configPublishedAt)
        && Objects.equals(flagsETag, that.flagsETag)
        && Objects.deepEquals(flagConfigJson, that.flagConfigJson)
        && Objects.deepEquals(banditParamsJson, that.banditParamsJson);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        banditReferences,
        flags,
        bandits,
        isConfigObfuscated,
        environmentName,
        configFetchedAt,
        configPublishedAt,
        flagsETag,
        Arrays.hashCode(flagConfigJson),
        Arrays.hashCode(banditParamsJson));
  }

  public IFlagConfig getFlag(String flagKey) {
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
    IFlagConfig flag = getFlag(flagKey);
    if (flag == null) {
      return null;
    }
    return flag.getVariationType();
  }

  public String banditKeyForVariation(String flagKey, String variationValue) {
    // Note: In practice this double loop should be quite quick as the number of bandits and bandit
    // variations will be small. Should this ever change, we can optimize things.
    for (Map.Entry<String, ? extends IBanditReference> banditEntry : banditReferences.entrySet()) {
      IBanditReference banditReference = banditEntry.getValue();
      for (IBanditFlagVariation banditFlagVariation : banditReference.getFlagVariations()) {
        if (banditFlagVariation.getFlagKey().equals(flagKey)
            && banditFlagVariation.getVariationValue().equals(variationValue)) {
          return banditEntry.getKey();
        }
      }
    }
    return null;
  }

  public IBanditParameters getBanditParameters(String banditKey) {
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
    return flags == null ? Collections.emptySet() : flags.keySet();
  }

  public String getEnvironmentName() {
    return environmentName;
  }

  public Date getConfigFetchedAt() {
    return configFetchedAt;
  }

  public Date getConfigPublishedAt() {
    return configPublishedAt;
  }

  /**
   * Get the ETag for the flags configuration. Used for HTTP caching via If-None-Match headers.
   *
   * @return ETag value or null if not set
   */
  @Nullable
  public String getFlagsETag() {
    return flagsETag;
  }

  public static Builder builder(byte[] flagJson) {
    return new Builder(flagJson);
  }

  /**
   * Builder to create the immutable config object.
   *
   * @see Configuration for usage.
   */
  public static class Builder {

    private final boolean isConfigObfuscated;
    private final Map<String, ? extends IFlagConfig> flags;
    private final Map<String, ? extends IBanditReference> banditReferences;
    private Map<String, ? extends IBanditParameters> bandits = Collections.emptyMap();
    private final byte[] flagJson;
    private byte[] banditParamsJson;
    private final String environmentName;
    private final Date configPublishedAt;
    private String flagsETag;

    private static IFlagConfigResponse parseFlagResponse(byte[] flagJson) {
      if (flagJson == null || flagJson.length == 0) {
        log.warn("Null or empty configuration string. Call `Configuration.Empty()` instead");
        return null;
      }
      try {
        return mapper.readValue(flagJson, FlagConfigResponse.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public Builder(byte[] flagJson) {
      this(flagJson, parseFlagResponse(flagJson));
    }

    public Builder(byte[] flagJson, IFlagConfigResponse flagConfigResponse) {
      this(
          flagJson,
          flagConfigResponse,
          flagConfigResponse.getFormat() == IFlagConfigResponse.Format.CLIENT);
    }

    public Builder(
        byte[] flagJson,
        @Nullable IFlagConfigResponse flagConfigResponse,
        boolean isConfigObfuscated) {
      this.isConfigObfuscated = isConfigObfuscated;
      this.flagJson = flagJson;
      if (flagConfigResponse == null || flagConfigResponse.getFlags() == null) {
        log.warn("'flags' map missing in flag definition JSON");
        flags = Collections.emptyMap();
        banditReferences = Collections.emptyMap();
        environmentName = null;
        configPublishedAt = null;
      } else {
        flags = Collections.unmodifiableMap(flagConfigResponse.getFlags());
        banditReferences = Collections.unmodifiableMap(flagConfigResponse.getBanditReferences());

        // Extract environment name and published at timestamp from the response
        environmentName = flagConfigResponse.getEnvironmentName();
        configPublishedAt = flagConfigResponse.getCreatedAt();

        log.debug("Loaded {} flag definitions from flag definition JSON", flags.size());
      }
    }

    public boolean requiresUpdatedBanditModels() {
      Set<String> neededModelVersions = referencedBanditModelVersion();
      return !loadedBanditModelVersions().containsAll(neededModelVersions);
    }

    public Set<String> loadedBanditModelVersions() {
      return bandits.values().stream()
          .map(IBanditParameters::getModelVersion)
          .collect(Collectors.toSet());
    }

    public Set<String> referencedBanditModelVersion() {
      return banditReferences.values().stream()
          .map(IBanditReference::getModelVersion)
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
      // Record the time when configuration is built/fetched
      Date configFetchedAt = new Date();
      return new Configuration(
          flags,
          banditReferences,
          bandits,
          isConfigObfuscated,
          environmentName,
          configFetchedAt,
          configPublishedAt,
          flagsETag,
          flagJson,
          banditParamsJson);
    }

    /**
     * Set the ETag for the flags configuration. Used for HTTP caching via If-None-Match headers.
     *
     * @param flagsETag ETag value from HTTP response
     * @return this Builder for chaining
     */
    public Builder flagsETag(String flagsETag) {
      this.flagsETag = flagsETag;
      return this;
    }
  }
}

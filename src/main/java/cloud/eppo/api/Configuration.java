package cloud.eppo.api;

import static cloud.eppo.Utils.getMD5Hex;

import cloud.eppo.api.dto.BanditFlagVariation;
import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.BanditReference;
import cloud.eppo.api.dto.FlagConfig;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.api.dto.VariationType;
import cloud.eppo.parser.ConfigurationParser;
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
 * <p>A Builder is used to prepare and then create an immutable data structure containing both flag
 * and bandit configurations. An intermediate step is required in building the configuration to
 * accommodate the as-needed loading of bandit parameters as a network call may not be needed if
 * there are no bandits referenced by the flag configuration.
 *
 * <p>Usage: Building with just flag configuration (unobfuscated is default) <code>
 *     Configuration config = Configuration.builder(flagJson, parser).build();
 * </code>
 *
 * <p>Building with bandits (known configuration) <code>
 *     Configuration config = Configuration.builder(flagJson, parser).banditParameters(banditJson).build();
 *     </code>
 *
 * <p>Conditionally loading bandit models (with or without an existing bandit config JSON string).
 * <code>
 *  Configuration.Builder configBuilder = Configuration.builder(flagJson, parser).banditParameters(banditJson);
 *  if (configBuilder.requiresUpdatedBanditModels()) {
 *    // Load the bandit parameters encoded in a JSON string
 *    configBuilder.banditParameters(banditParameterJson);
 *  }
 *  Configuration config = configBuilder.build();
 * </code>
 *
 * <p>
 *
 * <p>Hint: when loading new Flag configuration values, set the current bandit models in the builder
 * then check `requiresUpdatedBanditModels()`.
 */
public class Configuration {

  private static final byte[] emptyFlagsBytes =
      "{ \"flags\": {}, \"format\": \"SERVER\" }".getBytes();

  private static final Logger log = LoggerFactory.getLogger(Configuration.class);
  private final Map<String, BanditReference> banditReferences;
  private final Map<String, ? extends FlagConfig> flags;
  private final Map<String, ? extends BanditParameters> bandits;
  private final boolean isConfigObfuscated;
  private final String environmentName;
  private final Date configFetchedAt;
  private final Date configPublishedAt;
  private final String flagsVersionId;

  private final byte[] flagConfigJson;

  private final byte[] banditParamsJson;

  /** Default visibility for tests. */
  Configuration(
      Map<String, ? extends FlagConfig> flags,
      Map<String, BanditReference> banditReferences,
      Map<String, ? extends BanditParameters> bandits,
      boolean isConfigObfuscated,
      String environmentName,
      Date configFetchedAt,
      Date configPublishedAt,
      String flagsVersionId,
      byte[] flagConfigJson,
      byte[] banditParamsJson) {
    this.flags = flags;
    this.banditReferences = banditReferences;
    this.bandits = bandits;
    this.isConfigObfuscated = isConfigObfuscated;
    this.environmentName = environmentName;
    this.configFetchedAt = configFetchedAt;
    this.configPublishedAt = configPublishedAt;
    this.flagsVersionId = flagsVersionId;
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
        + ", flagsVersionId='"
        + flagsVersionId
        + '\''
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
        && Objects.equals(flagsVersionId, that.flagsVersionId)
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
        flagsVersionId,
        Arrays.hashCode(flagConfigJson),
        Arrays.hashCode(banditParamsJson));
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
   * Returns the version ID (ETag) of the flags configuration.
   *
   * <p>This can be used for conditional HTTP requests to avoid re-downloading unchanged
   * configurations.
   *
   * @return the flags version ID, or null if not set
   */
  public String getFlagsVersionId() {
    return flagsVersionId;
  }

  public static Builder builder(byte[] flagJson, ConfigurationParser parser) {
    return new Builder(flagJson, parser);
  }

  /**
   * Builder to create the immutable config object.
   *
   * @see Configuration for usage.
   */
  public static class Builder {

    private final ConfigurationParser parser;
    private final boolean isConfigObfuscated;
    private final Map<String, ? extends FlagConfig> flags;
    private final Map<String, BanditReference> banditReferences;
    private Map<String, ? extends BanditParameters> bandits = Collections.emptyMap();
    private final byte[] flagJson;
    private byte[] banditParamsJson;
    private final String environmentName;
    private final Date configPublishedAt;
    private String flagsVersionId;

    public Builder(byte[] flagJson, ConfigurationParser parser) {
      this(flagJson, parser, parseFlagResponse(flagJson, parser));
    }

    public Builder(
        byte[] flagJson, ConfigurationParser parser, FlagConfigResponse flagConfigResponse) {
      this(
          flagJson,
          parser,
          flagConfigResponse,
          flagConfigResponse != null
              && flagConfigResponse.getFormat() == FlagConfigResponse.Format.CLIENT);
    }

    public Builder(
        byte[] flagJson,
        ConfigurationParser parser,
        @Nullable FlagConfigResponse flagConfigResponse,
        boolean isConfigObfuscated) {
      this.parser = parser;
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

    private static FlagConfigResponse parseFlagResponse(
        byte[] flagJson, ConfigurationParser parser) {
      if (flagJson == null || flagJson.length == 0) {
        log.warn("Null or empty configuration string. Call `Configuration.emptyConfig()` instead");
        return null;
      }
      return parser.parseFlagConfig(flagJson);
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
      this.banditParamsJson = banditParameterJson;
      Map<String, ? extends BanditParameters> parsedBandits =
          parser.parseBanditParams(banditParameterJson);

      if (parsedBandits == null) {
        log.warn("`bandits` map missing in bandit parameters JSON");
        bandits = Collections.emptyMap();
      } else {
        bandits = Collections.unmodifiableMap(parsedBandits);
        log.debug("Loaded {} bandit models from bandit parameters JSON", bandits.size());
      }

      return this;
    }

    /**
     * Sets the flags version ID (ETag) for conditional fetching.
     *
     * @param flagsVersionId the ETag from the HTTP response
     * @return this builder
     */
    public Builder flagsVersionId(String flagsVersionId) {
      this.flagsVersionId = flagsVersionId;
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
          flagsVersionId,
          flagJson,
          banditParamsJson);
    }
  }
}

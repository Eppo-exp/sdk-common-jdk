package cloud.eppo.api;

import static cloud.eppo.Utils.getMD5Hex;

import cloud.eppo.api.dto.BanditFlagVariation;
import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.BanditReference;
import cloud.eppo.api.dto.FlagConfig;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.api.dto.VariationType;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
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
 * <p>Usage: Building with just flag configuration (obfuscation auto-detected from format):
 *
 * <pre>{@code
 * Configuration config = new Configuration.Builder(flagConfigBytes, null, null).build();
 * }</pre>
 *
 * <p>Building with bandits:
 *
 * <pre>{@code
 * Configuration.Builder builder = new Configuration.Builder(flagConfigResponse);
 * builder.banditParameters(banditParamsResponse);
 * Configuration config = builder.build();
 * }</pre>
 */
public class Configuration implements SerializableEppoConfiguration {
  private static final long serialVersionUID = 1L;
  private static final Logger log = LoggerFactory.getLogger(Configuration.class);
  final Map<String, BanditReference> banditReferences;
  private final Map<String, FlagConfig> flags;
  final Map<String, BanditParameters> bandits;
  private final boolean isConfigObfuscated;
  private final String environmentName;
  private final Date configFetchedAt;
  private final Date configPublishedAt;
  @Nullable private final String flagsSnapshotId;

  /** Default visibility for tests. */
  Configuration(
      Map<String, FlagConfig> flags,
      Map<String, BanditReference> banditReferences,
      Map<String, BanditParameters> bandits,
      boolean isConfigObfuscated,
      String environmentName,
      Date configFetchedAt,
      Date configPublishedAt,
      @Nullable String flagsSnapshotId) {
    this.flags = flags;
    this.banditReferences = banditReferences;
    this.bandits = bandits;
    this.isConfigObfuscated = isConfigObfuscated;
    this.environmentName = environmentName;
    this.configFetchedAt = configFetchedAt;
    this.configPublishedAt = configPublishedAt;
    this.flagsSnapshotId = flagsSnapshotId;
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
        + ", flagsSnapshotId="
        + flagsSnapshotId
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
        && Objects.equals(flagsSnapshotId, that.flagsSnapshotId);
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
        flagsSnapshotId);
  }

  @Override
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
  @Override
  public @Nullable VariationType getFlagType(String flagKey) {
    FlagConfig flag = getFlag(flagKey);
    if (flag == null) {
      return null;
    }
    return flag.getVariationType();
  }

  @Override
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

  @Override
  public BanditParameters getBanditParameters(String banditKey) {
    return bandits.get(banditKey);
  }

  @Override
  public boolean isConfigObfuscated() {
    return isConfigObfuscated;
  }

  @Override
  public boolean isEmpty() {
    return flags == null || flags.isEmpty();
  }

  @Override
  public Set<String> getFlagKeys() {
    return flags == null ? Collections.emptySet() : flags.keySet();
  }

  @Override
  public String getEnvironmentName() {
    return environmentName;
  }

  @Override
  public Date getConfigFetchedAt() {
    return configFetchedAt;
  }

  @Override
  public Date getConfigPublishedAt() {
    return configPublishedAt;
  }

  /**
   * Returns the snapshot ID for the flags configuration.
   *
   * <p>The snapshot ID is an opaque identifier (typically an HTTP ETag value) that represents a
   * specific version of the flag configuration. This value can be used for caching and conditional
   * requests to avoid re-fetching unchanged configuration data.
   *
   * @return the snapshot ID, or null if not available
   */
  @Override
  @Nullable public String getFlagsSnapshotId() {
    return flagsSnapshotId;
  }

  public static Builder builder(FlagConfigResponse flagConfigResponse) {
    return new Builder(flagConfigResponse);
  }

  /**
   * Returns a new Builder that is pre-populated with the data from this configuration, allowing
   * selective updates (e.g. applying new bandit parameters).
   */
  @NotNull public Builder toBuilder() {
    return new Builder(this);
  }

  /** Returns the bandit references map. Package-level detail exposed for parser implementations. */
  public Map<String, BanditReference> getBanditReferences() {
    return banditReferences;
  }

  /**
   * Returns the loaded bandit parameters map. Package-level detail exposed for parser
   * implementations.
   */
  public Map<String, BanditParameters> getBandits() {
    return bandits;
  }

  /**
   * Builder to create the immutable config object.
   *
   * @see Configuration for usage.
   */
  public static class Builder {
    private static final Logger log = LoggerFactory.getLogger(Builder.class);

    private final boolean isConfigObfuscated;
    private final Map<String, FlagConfig> flags;
    private final Map<String, BanditReference> banditReferences;
    private Map<String, BanditParameters> bandits = Collections.emptyMap();
    private final String environmentName;
    private final Date configPublishedAt;
    @Nullable private String flagsSnapshotId;
    @Nullable private Date configFetchedAt;

    public Builder(FlagConfigResponse flagConfigResponse) {
      this(flagConfigResponse, flagConfigResponse.getFormat() == FlagConfigResponse.Format.CLIENT);
    }

    public Builder(@Nullable FlagConfigResponse flagConfigResponse, boolean isConfigObfuscated) {
      this.isConfigObfuscated = isConfigObfuscated;
      if (flagConfigResponse == null || flagConfigResponse.getFlags() == null) {
        log.warn("'flags' map missing in flag definition JSON");
        flags = Collections.emptyMap();
        banditReferences = Collections.emptyMap();
        environmentName = null;
        configPublishedAt = null;
      } else {
        flags = Collections.unmodifiableMap(flagConfigResponse.getFlags());
        banditReferences = Collections.unmodifiableMap(flagConfigResponse.getBanditReferences());
        environmentName = flagConfigResponse.getEnvironmentName();
        configPublishedAt = flagConfigResponse.getCreatedAt();
        log.debug("Loaded {} flag definitions from flag definition JSON", flags.size());
      }
    }

    /** Copy constructor — reconstructs a Builder from an existing Configuration. */
    private Builder(@NotNull Configuration existing) {
      this.isConfigObfuscated = existing.isConfigObfuscated;
      this.flags = existing.flags;
      this.banditReferences = existing.banditReferences;
      this.bandits = existing.bandits;
      this.environmentName = existing.environmentName;
      this.configPublishedAt = existing.configPublishedAt;
      this.flagsSnapshotId = existing.flagsSnapshotId;
      this.configFetchedAt = existing.getConfigFetchedAt();
    }

    /** Carry over bandit parameters from an existing configuration (if non-null). */
    public Builder banditParametersFromConfig(@Nullable Configuration currentConfig) {
      if (currentConfig == null || currentConfig.bandits == null) {
        bandits = Collections.emptyMap();
      } else {
        bandits = currentConfig.bandits;
      }
      return this;
    }

    public Builder banditParameters(
        @Nullable cloud.eppo.api.dto.BanditParametersResponse banditParametersResponse) {
      if (banditParametersResponse == null || banditParametersResponse.getBandits() == null) {
        bandits = Collections.emptyMap();
        return this;
      }
      bandits = Collections.unmodifiableMap(banditParametersResponse.getBandits());
      return this;
    }

    public Builder flagsSnapshotId(@Nullable String flagsSnapshotId) {
      this.flagsSnapshotId = flagsSnapshotId;
      return this;
    }

    public Configuration build() {
      Date resolvedConfigFetchedAt =
          this.configFetchedAt != null ? this.configFetchedAt : new Date();
      return new Configuration(
          flags,
          banditReferences,
          bandits,
          isConfigObfuscated,
          environmentName,
          resolvedConfigFetchedAt,
          configPublishedAt,
          flagsSnapshotId);
    }
  }
}

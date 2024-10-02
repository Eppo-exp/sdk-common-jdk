package cloud.eppo.api;

import static cloud.eppo.Utils.getMD5Hex;

import cloud.eppo.ufc.dto.*;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
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

  @SuppressWarnings("unused")
  private final byte[] flagConfigJson;

  private final byte[] banditParamsJson;

  private Configuration(
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
    this.flagConfigJson = flagConfigJson;
    this.banditParamsJson = banditParamsJson;
  }

  public static Configuration emptyConfig() {
    return new Configuration(
        Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false, null, null);
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

  public byte[] serializeFlagConfigToBytes() {
    return flagConfigJson;
  }

  public byte[] serializeBanditParamsToBytes() {
    return banditParamsJson;
  }

  public boolean isEmpty() {
    return flags == null || flags.isEmpty();
  }

  public static Builder builder(byte[] flagJson, boolean isConfigObfuscated) {
    return new Builder(flagJson, isConfigObfuscated);
  }
  /**
   * Builder to create the immutable config object.
   *
   * @see Configuration for usage.
   */
  public static class Builder {
    private static final ObjectMapper mapper =
        new ObjectMapper().registerModule(EppoModule.eppoModule());

    private final boolean isConfigObfuscated;
    private final Map<String, FlagConfig> flags;
    private Map<String, BanditReference> banditReferences;
    private Map<String, BanditParameters> bandits = Collections.emptyMap();
    private final byte[] flagJson;
    private byte[] banditParamsJson;

    public Builder(String flagJson, boolean isConfigObfuscated) {
      this(flagJson.getBytes(), isConfigObfuscated);
    }

    public Builder(byte[] flagJson, boolean isConfigObfuscated) {
      this.isConfigObfuscated = isConfigObfuscated;

      if (flagJson == null || flagJson.length == 0) {
        log.warn("Null or empty configuration string. Call `Configuration.Empty()` instead");
        flags = Collections.emptyMap();
        banditReferences = Collections.emptyMap();
        this.flagJson = null;
        return;
      }

      // Build the flags config from the json string.
      FlagConfigResponse config;
      try {
        config = mapper.readValue(flagJson, FlagConfigResponse.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      if (config == null || config.getFlags() == null) {
        log.warn("'flags' map missing in flag definition JSON");
        flags = Collections.emptyMap();
        this.flagJson = null;
      } else {
        flags = Collections.unmodifiableMap(config.getFlags());
        banditReferences = Collections.unmodifiableMap(config.getBanditReferences());
        this.flagJson = flagJson;
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

  private static class ConfigurationPacket implements Serializable {
    final byte[] flagConfigBytes;
    final byte[] banditParamsBytes;
    final boolean isConfigObfuscated;

    ConfigurationPacket(
        byte[] flagConfigBytes, byte[] banditParamsBytes, boolean isConfigObfuscated) {
      this.flagConfigBytes = flagConfigBytes;
      this.banditParamsBytes = banditParamsBytes;
      this.isConfigObfuscated = isConfigObfuscated;
    }
  }

  private ConfigurationPacket getConfigurationPacket() {
    return new ConfigurationPacket(flagConfigJson, banditParamsJson, isConfigObfuscated);
  }

  public void writeToStream(OutputStream out) throws IOException {
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject(getConfigurationPacket());
  }

  public static Configuration fromInputStream(InputStream inputStream) throws IOException {
    ObjectInputStream ois = new ObjectInputStream(inputStream);
    try {
      ConfigurationPacket packet = (ConfigurationPacket) ois.readObject();
      return Configuration.builder(packet.flagConfigBytes, packet.isConfigObfuscated)
          .banditParameters(packet.banditParamsBytes)
          .build();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}

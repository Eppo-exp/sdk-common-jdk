package cloud.eppo;

import cloud.eppo.api.Configuration;
import cloud.eppo.api.dto.BanditParametersResponse;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.parser.ConfigurationParseException;
import cloud.eppo.parser.ConfigurationParser;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ConfigurationParser} using Jackson.
 *
 * <p>This parser uses Jackson's ObjectMapper with custom deserializers for Eppo's configuration
 * format. The deserializers are hand-rolled to avoid reliance on annotations and method names,
 * which can be unreliable when ProGuard minification is in use.
 */
public class JacksonConfigurationParser implements ConfigurationParser<Configuration, JsonNode> {
  private static final Logger log = LoggerFactory.getLogger(JacksonConfigurationParser.class);

  private final ObjectMapper objectMapper;

  /** Creates a new parser with the default ObjectMapper configuration. */
  public JacksonConfigurationParser() {
    this(createDefaultObjectMapper());
  }

  /**
   * Creates a new parser with a custom ObjectMapper.
   *
   * <p>Note: The provided ObjectMapper must be configured with {@link EppoModule#eppoModule()} for
   * proper deserialization of Eppo configuration types.
   *
   * @param objectMapper the ObjectMapper instance to use
   */
  public JacksonConfigurationParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  private static ObjectMapper createDefaultObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(EppoModule.eppoModule());
    return mapper;
  }

  /**
   * Parses raw flag config bytes and builds a Configuration. Bandit parameters from previousConfig
   * are carried over. flagsSnapshotId (the HTTP ETag) is stored on the built config.
   */
  @Override
  @NotNull public Configuration buildConfig(
      @NotNull byte[] flagConfigBytes,
      @Nullable String flagsSnapshotId,
      @Nullable Configuration previousConfig) {
    try {
      log.debug("Parsing flag configuration, {} bytes", flagConfigBytes.length);
      FlagConfigResponse flagConfigResponse =
          objectMapper.readValue(flagConfigBytes, FlagConfigResponse.class);
      Configuration.Builder builder =
          new Configuration.Builder(flagConfigResponse).banditParametersFromConfig(previousConfig);
      builder.flagsSnapshotId(flagsSnapshotId);
      return builder.build();
    } catch (IOException e) {
      throw new RuntimeException(
          new ConfigurationParseException("Failed to parse flag configuration", e));
    }
  }

  /**
   * Parses bandit parameter bytes and returns a new Configuration with the updated bandits applied.
   * Uses {@link Configuration#toBuilder()} to copy all existing fields, then overwrites bandits.
   */
  @Override
  @NotNull public Configuration applyBanditParameters(
      @NotNull Configuration config, @NotNull byte[] banditParamsBytes) {
    try {
      log.debug("Parsing bandit parameters, {} bytes", banditParamsBytes.length);
      BanditParametersResponse banditParametersResponse =
          objectMapper.readValue(banditParamsBytes, BanditParametersResponse.class);
      return config.toBuilder().banditParameters(banditParametersResponse).build();
    } catch (IOException e) {
      throw new RuntimeException(
          new ConfigurationParseException("Failed to parse bandit parameters", e));
    }
  }

  /**
   * Parses raw flag config bytes (used externally where the full buildConfig flow isn't needed).
   */
  public FlagConfigResponse parseFlagConfig(byte[] flagConfigJson)
      throws ConfigurationParseException {
    try {
      log.debug("Parsing flag configuration, {} bytes", flagConfigJson.length);
      return objectMapper.readValue(flagConfigJson, FlagConfigResponse.class);
    } catch (IOException e) {
      throw new ConfigurationParseException("Failed to parse flag configuration", e);
    }
  }

  /** Parses raw bandit parameter bytes (used externally where applyBanditParameters isn't used). */
  public BanditParametersResponse parseBanditParams(byte[] banditParamsJson)
      throws ConfigurationParseException {
    try {
      log.debug("Parsing bandit parameters, {} bytes", banditParamsJson.length);
      return objectMapper.readValue(banditParamsJson, BanditParametersResponse.class);
    } catch (IOException e) {
      throw new ConfigurationParseException("Failed to parse bandit parameters", e);
    }
  }

  @Override
  @NotNull public JsonNode parseJsonValue(@NotNull String jsonValue) throws ConfigurationParseException {
    try {
      return objectMapper.readTree(jsonValue);
    } catch (IOException e) {
      throw new ConfigurationParseException("Failed to parse JSON value", e);
    }
  }
}

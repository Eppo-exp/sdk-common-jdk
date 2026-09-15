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

  /**
   * Creates a new parser with the default ObjectMapper configuration.
   *
   * @see #createDefaultObjectMapper()
   */
  public JacksonConfigurationParser() {
    this(createDefaultObjectMapper());
  }

  /**
   * Creates a new parser with a custom ObjectMapper.
   *
   * <p>The provided ObjectMapper must be configured with Eppo's custom deserializers. Use {@link
   * #createDefaultObjectMapper()} to obtain a correctly configured instance.
   *
   * @param objectMapper the ObjectMapper instance to use
   */
  public JacksonConfigurationParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Creates a new {@link ObjectMapper} configured with Eppo's custom deserializers.
   *
   * <p>Use this when you need a standalone mapper for deserializing Eppo types (e.g., {@link
   * cloud.eppo.api.EppoValue}) outside of a full parser instance. To configure an existing
   * ObjectMapper instead, use {@link #configureObjectMapper(ObjectMapper)}.
   */
  public static ObjectMapper createDefaultObjectMapper() {
    return configureObjectMapper(new ObjectMapper());
  }

  /**
   * Registers Eppo's custom deserializers on the provided {@link ObjectMapper} and returns it.
   *
   * <p>Use this when the caller already owns an ObjectMapper instance and wants to add Eppo support
   * to it without creating a new one.
   *
   * @param objectMapper the ObjectMapper to configure
   * @return the same ObjectMapper, for chaining
   */
  public static ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
    objectMapper.registerModule(EppoModule.eppoModule());
    return objectMapper;
  }

  /** Parses raw flag configuration JSON bytes into a {@link FlagConfigResponse}. */
  @Override
  @NotNull public FlagConfigResponse parseFlagConfig(@NotNull byte[] flagConfigBytes)
      throws ConfigurationParseException {
    try {
      log.debug("Parsing flag configuration, {} bytes", flagConfigBytes.length);
      return objectMapper.readValue(flagConfigBytes, FlagConfigResponse.class);
    } catch (IOException e) {
      throw new ConfigurationParseException("Failed to parse flag configuration", e);
    }
  }

  /**
   * Builds a Configuration from a parsed flag response, snapshot ID, previous config, and optional
   * fresh bandit parameter bytes. If {@code banditParamsBytes} is non-null, fresh bandit parameters
   * are parsed and applied. Otherwise, bandit parameters are carried over from {@code
   * previousConfig}.
   */
  @Override
  @NotNull public Configuration buildConfig(
      @NotNull FlagConfigResponse flags,
      @Nullable String flagsSnapshotId,
      @Nullable Configuration previousConfig,
      @Nullable byte[] banditParamsBytes) {
    Configuration.Builder builder = new Configuration.Builder(flags);
    if (previousConfig != null) {
      builder.banditParametersFromConfig(previousConfig);
    }
    builder.flagsSnapshotId(flagsSnapshotId);
    if (banditParamsBytes != null) {
      try {
        BanditParametersResponse banditResponse =
            objectMapper.readValue(banditParamsBytes, BanditParametersResponse.class);
        builder.banditParameters(banditResponse);
      } catch (IOException e) {
        throw new ConfigurationParseException("Failed to parse bandit parameters", e);
      }
    }
    return builder.build();
  }

  /** Parses raw bandit parameter bytes. */
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

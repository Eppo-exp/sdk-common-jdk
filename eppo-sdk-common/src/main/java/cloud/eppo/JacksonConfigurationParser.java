package cloud.eppo;

import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.BanditParametersResponse;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.parser.ConfigurationParseException;
import cloud.eppo.parser.ConfigurationParser;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ConfigurationParser} using Jackson.
 *
 * <p>This parser uses Jackson's ObjectMapper with custom deserializers for Eppo's configuration
 * format. The deserializers are hand-rolled to avoid reliance on annotations and method names,
 * which can be unreliable when ProGuard minification is in use.
 */
public class JacksonConfigurationParser implements ConfigurationParser {
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

  @Override
  public FlagConfigResponse parseFlagConfig(byte[] flagConfigJson)
      throws ConfigurationParseException {
    try {
      log.debug("Parsing flag configuration, {} bytes", flagConfigJson.length);
      return objectMapper.readValue(flagConfigJson, FlagConfigResponse.class);
    } catch (IOException e) {
      throw new ConfigurationParseException("Failed to parse flag configuration", e);
    }
  }

  @Override
  public Map<String, ? extends BanditParameters> parseBanditParams(byte[] banditParamsJson)
      throws ConfigurationParseException {
    try {
      log.debug("Parsing bandit parameters, {} bytes", banditParamsJson.length);
      BanditParametersResponse response =
          objectMapper.readValue(banditParamsJson, BanditParametersResponse.class);
      return response.getBandits();
    } catch (IOException e) {
      throw new ConfigurationParseException("Failed to parse bandit parameters", e);
    }
  }

  @Override
  public byte[] serializeFlagConfig(FlagConfigResponse flagConfigResponse)
      throws ConfigurationParseException {
    try {
      log.debug("Serializing flag configuration");
      return objectMapper.writeValueAsBytes(flagConfigResponse);
    } catch (JsonProcessingException e) {
      throw new ConfigurationParseException("Failed to serialize flag configuration", e);
    }
  }

  @Override
  public byte[] serializeBanditParams(Map<String, ? extends BanditParameters> banditParams)
      throws ConfigurationParseException {
    try {
      log.debug("Serializing bandit parameters");
      BanditParametersResponse response =
          new BanditParametersResponse.Default(castBanditMap(banditParams));
      return objectMapper.writeValueAsBytes(response);
    } catch (JsonProcessingException e) {
      throw new ConfigurationParseException("Failed to serialize bandit parameters", e);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, BanditParameters> castBanditMap(
      Map<String, ? extends BanditParameters> banditParams) {
    return (Map<String, BanditParameters>) banditParams;
  }
}

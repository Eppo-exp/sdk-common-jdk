package cloud.eppo;

import cloud.eppo.parser.ConfigurationParseException;
import cloud.eppo.parser.ConfigurationParser;
import cloud.eppo.ufc.dto.BanditParameters;
import cloud.eppo.ufc.dto.BanditParametersResponse;
import cloud.eppo.ufc.dto.FlagConfigResponse;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Jackson-based implementation of {@link ConfigurationParser}.
 *
 * <p>This implementation uses Jackson for JSON serialization and deserialization of flag
 * configuration and bandit parameters.
 */
public class JacksonConfigurationParser implements ConfigurationParser {

  private final ObjectMapper objectMapper;

  public JacksonConfigurationParser() {
    this.objectMapper = new ObjectMapper().registerModule(EppoModule.eppoModule());
  }

  @Override
  public FlagConfigResponse parseFlagConfig(byte[] flagConfigJson)
      throws ConfigurationParseException {
    if (flagConfigJson == null || flagConfigJson.length == 0) {
      throw new ConfigurationParseException("Flag config JSON is null or empty");
    }
    try {
      return objectMapper.readValue(flagConfigJson, FlagConfigResponse.class);
    } catch (IOException e) {
      throw new ConfigurationParseException("Failed to parse flag config JSON", e);
    }
  }

  @Override
  public Map<String, BanditParameters> parseBanditParams(byte[] banditParamsJson)
      throws ConfigurationParseException {
    if (banditParamsJson == null || banditParamsJson.length == 0) {
      return Collections.emptyMap();
    }
    try {
      BanditParametersResponse response =
          objectMapper.readValue(banditParamsJson, BanditParametersResponse.class);
      if (response == null || response.getBandits() == null) {
        return Collections.emptyMap();
      }
      return response.getBandits();
    } catch (IOException e) {
      throw new ConfigurationParseException("Failed to parse bandit params JSON", e);
    }
  }

  @Override
  public byte[] serializeFlagConfig(FlagConfigResponse flagConfigResponse)
      throws ConfigurationParseException {
    if (flagConfigResponse == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsBytes(flagConfigResponse);
    } catch (JsonProcessingException e) {
      throw new ConfigurationParseException("Failed to serialize flag config", e);
    }
  }

  @Override
  public byte[] serializeBanditParams(Map<String, BanditParameters> banditParams)
      throws ConfigurationParseException {
    if (banditParams == null || banditParams.isEmpty()) {
      return null;
    }
    try {
      BanditParametersResponse response = new BanditParametersResponse(banditParams);
      return objectMapper.writeValueAsBytes(response);
    } catch (JsonProcessingException e) {
      throw new ConfigurationParseException("Failed to serialize bandit params", e);
    }
  }
}

package cloud.eppo.helpers;

import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.BanditParametersResponse;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.parser.ConfigurationParseException;
import cloud.eppo.parser.ConfigurationParser;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Jackson-based ConfigurationParser implementation for tests.
 *
 * <p>This is a copy of JacksonConfigurationParser from eppo-sdk-common, provided here so tests in
 * the root module can compile and run.
 */
public class TestConfigurationParser implements ConfigurationParser {

  private final ObjectMapper objectMapper;

  public TestConfigurationParser() {
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
  public Map<String, ? extends BanditParameters> parseBanditParams(byte[] banditParamsJson)
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
  public byte[] serializeBanditParams(Map<String, ? extends BanditParameters> banditParams)
      throws ConfigurationParseException {
    if (banditParams == null || banditParams.isEmpty()) {
      return null;
    }
    try {
      // Note: BanditParametersResponse.Default expects Map<String, BanditParameters>
      // Since we're serializing, the concrete type doesn't matter for Jackson
      @SuppressWarnings("unchecked")
      Map<String, BanditParameters> concreteParams = (Map<String, BanditParameters>) banditParams;
      BanditParametersResponse response = new BanditParametersResponse.Default(concreteParams);
      return objectMapper.writeValueAsBytes(response);
    } catch (JsonProcessingException e) {
      throw new ConfigurationParseException("Failed to serialize bandit params", e);
    }
  }
}

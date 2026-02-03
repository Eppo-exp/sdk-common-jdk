package cloud.eppo.parser;

import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.FlagConfigResponse;
import java.util.Map;

/**
 * Interface for parsing configuration JSON responses.
 *
 * <p>Implementations of this interface handle deserialization of flag configuration and bandit
 * parameters from raw JSON bytes. The SDK provides a default implementation using Jackson in the
 * eppo-sdk-common module.
 */
public interface ConfigurationParser {

  /**
   * Parses raw flag configuration JSON bytes.
   *
   * @param flagConfigJson raw JSON bytes for flag configuration
   * @return parsed FlagConfigResponse containing flags, bandit references, format, etc.
   * @throws ConfigurationParseException if parsing fails
   */
  FlagConfigResponse parseFlagConfig(byte[] flagConfigJson) throws ConfigurationParseException;

  /**
   * Parses raw bandit parameters JSON bytes.
   *
   * @param banditParamsJson raw JSON bytes for bandit parameters
   * @return map of bandit key to BanditParameters
   * @throws ConfigurationParseException if parsing fails
   */
  Map<String, ? extends BanditParameters> parseBanditParams(byte[] banditParamsJson)
      throws ConfigurationParseException;

  /**
   * Serializes a FlagConfigResponse to JSON bytes.
   *
   * <p>This is used for caching and debugging purposes.
   *
   * @param flagConfigResponse the response to serialize
   * @return JSON bytes representing the flag configuration
   * @throws ConfigurationParseException if serialization fails
   */
  byte[] serializeFlagConfig(FlagConfigResponse flagConfigResponse)
      throws ConfigurationParseException;

  /**
   * Serializes bandit parameters to JSON bytes.
   *
   * <p>This is used for caching and debugging purposes.
   *
   * @param banditParams map of bandit key to BanditParameters
   * @return JSON bytes representing the bandit parameters
   * @throws ConfigurationParseException if serialization fails
   */
  byte[] serializeBanditParams(Map<String, ? extends BanditParameters> banditParams)
      throws ConfigurationParseException;
}

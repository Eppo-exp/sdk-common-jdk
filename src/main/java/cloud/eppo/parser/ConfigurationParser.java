package cloud.eppo.parser;

import cloud.eppo.api.dto.BanditParametersResponse;
import cloud.eppo.api.dto.FlagConfigResponse;

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
   * @return parsed BanditParametersResponse containing bandit models
   * @throws ConfigurationParseException if parsing fails
   */
  BanditParametersResponse parseBanditParams(byte[] banditParamsJson)
      throws ConfigurationParseException;
}

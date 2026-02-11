package cloud.eppo.parser;

import cloud.eppo.api.dto.BanditParametersResponse;
import cloud.eppo.api.dto.FlagConfigResponse;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the contract for parsing configuration JSON responses.
 *
 * <p>Implementations of this interface handle deserialization of flag configuration and bandit
 * parameters from raw JSON bytes. The SDK includes a default implementation using Jackson (in the
 * eppo-sdk-common module), but users can supply custom implementations to accommodate specialized
 * needs.
 */
public interface ConfigurationParser {

  /**
   * Parses raw flag configuration JSON bytes.
   *
   * @param flagConfigJson raw JSON bytes for flag configuration
   * @return parsed FlagConfigResponse containing flags, bandit references, format, etc.
   * @throws ConfigurationParseException if parsing fails
   */
  @NotNull FlagConfigResponse parseFlagConfig(@NotNull byte[] flagConfigJson)
      throws ConfigurationParseException;

  /**
   * Parses raw bandit parameters JSON bytes.
   *
   * @param banditParamsJson raw JSON bytes for bandit parameters
   * @return parsed BanditParametersResponse containing bandit models
   * @throws ConfigurationParseException if parsing fails
   */
  @NotNull BanditParametersResponse parseBanditParams(@NotNull byte[] banditParamsJson)
      throws ConfigurationParseException;
}

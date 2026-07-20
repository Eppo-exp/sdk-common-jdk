package cloud.eppo.parser;

import cloud.eppo.api.SerializableEppoConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the contract for parsing configuration JSON responses.
 *
 * <p>Implementations of this interface handle deserialization of flag configuration and bandit
 * parameters from raw JSON bytes, as well as building and updating configuration objects. The SDK
 * includes a default implementation using Jackson (in the eppo-sdk-common module), but users can
 * supply custom implementations to accommodate specialized needs.
 */
public interface ConfigurationParser<
    ConfigurationType extends SerializableEppoConfiguration, JSONFlagType> {

  /**
   * Parses raw flag config bytes and builds a configuration object. Bandit parameters from
   * previousConfig are carried over if still valid. flagsSnapshotId comes from the HTTP ETag header
   * (may be null). previousConfig is null on first fetch.
   *
   * @param flagConfigBytes raw JSON bytes for flag configuration
   * @param flagsSnapshotId opaque snapshot ID from HTTP ETag header, or null
   * @param previousConfig previous configuration to carry over bandit parameters, or null
   * @return a new configuration object built from the flag config bytes
   */
  @NotNull ConfigurationType buildConfig(
      @NotNull byte[] flagConfigBytes,
      @Nullable String flagsSnapshotId,
      @Nullable ConfigurationType previousConfig);

  /**
   * Returns true if the config references bandit model versions not yet loaded. Lives on parser
   * (not on SerializableEppoConfiguration) so config implementors don't need to implement bandit
   * logic.
   *
   * @param config the configuration to check
   * @return true if updated bandit models are needed
   */
  boolean requiresUpdatedBanditModels(@NotNull ConfigurationType config);

  /**
   * Applies freshly fetched bandit parameters to the config, returning an updated config. Parser
   * owns the bytes-to-config transformation; BanditParametersResponse is an implementation detail.
   *
   * @param config the configuration to update
   * @param banditParamsBytes raw JSON bytes for bandit parameters
   * @return an updated configuration with the new bandit parameters applied
   */
  @NotNull ConfigurationType applyBanditParameters(
      @NotNull ConfigurationType config, @NotNull byte[] banditParamsBytes);

  /**
   * Unwraps a JSON value to the appropriate JSONFlagType.
   *
   * @param jsonValue the encoded JSON value
   * @return the parsed JSON value
   * @throws ConfigurationParseException if unwrapping fails
   */
  @NotNull JSONFlagType parseJsonValue(@NotNull String jsonValue) throws ConfigurationParseException;
}

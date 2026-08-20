package cloud.eppo.parser;

import cloud.eppo.api.SerializableEppoConfiguration;
import cloud.eppo.api.dto.FlagConfigResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the contract for parsing configuration JSON responses and building configuration objects.
 *
 * <p>Implementations handle deserialization of flag configuration and bandit parameters from raw
 * JSON bytes, as well as building immutable configuration objects. The SDK includes a default
 * implementation using Jackson (in the eppo-sdk-common module), but users can supply custom
 * implementations to accommodate specialized needs.
 */
public interface ConfigurationParser<
    ConfigurationType extends SerializableEppoConfiguration, JSONFlagType> {

  /**
   * Parses raw flag configuration JSON bytes into a {@link FlagConfigResponse}.
   *
   * @param flagConfigBytes raw JSON bytes for flag configuration
   * @return parsed FlagConfigResponse containing flags, bandit references, format, etc.
   * @throws ConfigurationParseException if parsing fails
   */
  @NotNull FlagConfigResponse parseFlagConfig(@NotNull byte[] flagConfigBytes)
      throws ConfigurationParseException;

  /**
   * Builds an immutable configuration object from a parsed flag response, snapshot ID, previous
   * configuration, and optional fresh bandit parameters.
   *
   * <p>If {@code banditParamsBytes} is non-null, fresh bandit parameters are applied. If null,
   * bandit parameters are carried over from {@code previousConfig} (if available). Pass null when
   * the caller has determined fresh bandit parameters are not needed.
   *
   * @param flags parsed flag configuration response
   * @param flagsSnapshotId opaque snapshot ID from HTTP ETag header, or null
   * @param previousConfig previous configuration to carry over bandit parameters from, or null on
   *     first fetch
   * @param banditParamsBytes raw JSON bytes for fresh bandit parameters, or null if not needed
   * @return a new immutable configuration object
   */
  @NotNull ConfigurationType buildConfig(
      @NotNull FlagConfigResponse flags,
      @Nullable String flagsSnapshotId,
      @Nullable ConfigurationType previousConfig,
      @Nullable byte[] banditParamsBytes);

  /**
   * Unwraps a JSON value string to the appropriate JSONFlagType.
   *
   * @param jsonValue the encoded JSON value
   * @return the parsed JSON value
   * @throws ConfigurationParseException if unwrapping fails
   */
  @NotNull JSONFlagType parseJsonValue(@NotNull String jsonValue) throws ConfigurationParseException;
}

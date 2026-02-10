package cloud.eppo;

import cloud.eppo.api.AssignmentDetails;
import cloud.eppo.api.Attributes;
import cloud.eppo.api.Configuration;
import cloud.eppo.api.EppoValue;
import cloud.eppo.api.EvaluationDetails;
import cloud.eppo.api.FlagEvaluationCode;
import cloud.eppo.api.IAssignmentCache;
import cloud.eppo.api.dto.VariationType;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditLogger;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Eppo client implementation that uses OkHttp for HTTP communication and Jackson for JSON parsing.
 *
 * <p>This client extends {@link BaseEppoClient} and provides default implementations of the HTTP
 * client and configuration parser using OkHttp 4 and Jackson respectively.
 *
 * <p>For custom HTTP client or parser implementations, depend on {@code
 * cloud.eppo:eppo-sdk-framework} instead and use {@link BaseEppoClient} directly with your own
 * implementations of {@link cloud.eppo.http.EppoConfigurationClient} and {@link
 * cloud.eppo.parser.ConfigurationParser}.
 */
public class CommonEppoClient extends BaseEppoClient {

  /**
   * Creates a new CommonEppoClient with the specified configuration.
   *
   * @param apiKey the API key for authentication with Eppo servers
   * @param sdkName the SDK name for request metadata (e.g., "java-server-sdk")
   * @param sdkVersion the SDK version for request metadata
   * @param apiBaseUrl the base URL for API requests, or null for the default
   * @param assignmentLogger logger for assignment events, or null to disable logging
   * @param banditLogger logger for bandit assignment events, or null to disable logging
   * @param configurationStore custom configuration store, or null for the default in-memory store
   * @param isGracefulMode if true, errors during evaluation return default values instead of
   *     throwing
   * @param expectObfuscatedConfig if true, expect obfuscated configuration (for Android clients)
   * @param supportBandits if true, enable bandit support
   * @param initialConfiguration future providing initial configuration, or null
   * @param assignmentCache cache for deduplicating assignment logs, or null to disable
   * @param banditAssignmentCache cache for deduplicating bandit assignment logs, or null to disable
   */
  protected CommonEppoClient(
      @NotNull String apiKey,
      @NotNull String sdkName,
      @NotNull String sdkVersion,
      @Nullable String apiBaseUrl,
      @Nullable AssignmentLogger assignmentLogger,
      @Nullable BanditLogger banditLogger,
      @Nullable IConfigurationStore configurationStore,
      boolean isGracefulMode,
      boolean expectObfuscatedConfig,
      boolean supportBandits,
      @Nullable CompletableFuture<Configuration> initialConfiguration,
      @Nullable IAssignmentCache assignmentCache,
      @Nullable IAssignmentCache banditAssignmentCache) {
    super(
        apiKey,
        sdkName,
        sdkVersion,
        apiBaseUrl,
        assignmentLogger,
        banditLogger,
        configurationStore,
        isGracefulMode,
        expectObfuscatedConfig,
        supportBandits,
        initialConfiguration,
        assignmentCache,
        banditAssignmentCache,
        new JacksonConfigurationParser(),
        new OkHttpEppoClient());
  }

  // ==================== JSON Assignment Methods (Jackson-dependent) ====================

  public JsonNode getJSONAssignment(String flagKey, String subjectKey, JsonNode defaultValue) {
    return getJSONAssignment(flagKey, subjectKey, new Attributes(), defaultValue);
  }

  public JsonNode getJSONAssignment(
      String flagKey, String subjectKey, Attributes subjectAttributes, JsonNode defaultValue) {
    return this.getJSONAssignmentDetails(flagKey, subjectKey, subjectAttributes, defaultValue)
        .getVariation();
  }

  public AssignmentDetails<JsonNode> getJSONAssignmentDetails(
      String flagKey, String subjectKey, JsonNode defaultValue) {
    return this.getJSONAssignmentDetails(flagKey, subjectKey, new Attributes(), defaultValue);
  }

  public AssignmentDetails<JsonNode> getJSONAssignmentDetails(
      String flagKey, String subjectKey, Attributes subjectAttributes, JsonNode defaultValue) {
    try {
      return this.getTypedAssignmentWithDetails(
          flagKey, subjectKey, subjectAttributes, defaultValue, VariationType.JSON);
    } catch (Exception e) {
      String defaultValueString = defaultValue != null ? defaultValue.toString() : null;
      return new AssignmentDetails<>(
          throwIfNotGraceful(e, defaultValue),
          null,
          EvaluationDetails.buildDefault(
              getConfiguration().getEnvironmentName(),
              getConfiguration().getConfigFetchedAt(),
              getConfiguration().getConfigPublishedAt(),
              FlagEvaluationCode.ASSIGNMENT_ERROR,
              e.getMessage(),
              EppoValue.valueOf(defaultValueString)));
    }
  }
}

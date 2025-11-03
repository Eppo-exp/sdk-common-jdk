package cloud.eppo;

import static cloud.eppo.Constants.DEFAULT_JITTER_INTERVAL_RATIO;
import static cloud.eppo.Constants.DEFAULT_POLLING_INTERVAL_MILLIS;
import static cloud.eppo.Utils.throwIfEmptyOrNull;
import static cloud.eppo.Utils.throwIfNull;
import static cloud.eppo.ValuedFlagEvaluationResultType.BAD_VALUE_TYPE;
import static cloud.eppo.ValuedFlagEvaluationResultType.BAD_VARIATION_TYPE;
import static cloud.eppo.ValuedFlagEvaluationResultType.FLAG_DISABLED;
import static cloud.eppo.ValuedFlagEvaluationResultType.NO_ALLOCATION;
import static cloud.eppo.ValuedFlagEvaluationResultType.NO_FLAG_CONFIG;
import static cloud.eppo.ValuedFlagEvaluationResultType.OK;

import cloud.eppo.api.*;
import cloud.eppo.cache.AssignmentCacheEntry;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditAssignment;
import cloud.eppo.logging.BanditLogger;
import cloud.eppo.ufc.dto.*;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseEppoClient {
  private static final Logger log = LoggerFactory.getLogger(BaseEppoClient.class);
  private final ObjectMapper mapper =
      new ObjectMapper()
          .registerModule(EppoModule.eppoModule()); // TODO: is this the best place for this?

  protected final ConfigurationRequestor requestor;

  private final IConfigurationStore configurationStore;
  private final AssignmentLogger assignmentLogger;
  private final BanditLogger banditLogger;
  private final String sdkName;
  private final String sdkVersion;
  private boolean isGracefulMode;
  private final IAssignmentCache assignmentCache;
  private final IAssignmentCache banditAssignmentCache;
  private Timer pollTimer;

  @Nullable protected CompletableFuture<Boolean> getInitialConfigFuture() {
    return initialConfigFuture;
  }

  private final CompletableFuture<Boolean> initialConfigFuture;

  // Fields useful for testing in situations where we want to mock the http client or configuration
  // store (accessed via reflection)
  /** @noinspection FieldMayBeFinal */
  private static EppoHttpClient httpClientOverride = null;

  // It is important that the bandit assignment cache expire with a short-enough TTL to last about
  // one user session.
  // The recommended is 10 minutes (per @Sven)
  /** @param host To be removed in v4. use `apiBaseUrl` instead. */
  protected BaseEppoClient(
      @NotNull String apiKey,
      @NotNull String sdkName,
      @NotNull String sdkVersion,
      @Deprecated @Nullable String host,
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

    if (apiBaseUrl == null) {
      apiBaseUrl = host != null ? Constants.appendApiPathToHost(host) : Constants.DEFAULT_BASE_URL;
    }

    this.assignmentCache = assignmentCache;
    this.banditAssignmentCache = banditAssignmentCache;

    EppoHttpClient httpClient =
        buildHttpClient(apiBaseUrl, new SDKKey(apiKey), sdkName, sdkVersion);
    this.configurationStore =
        configurationStore != null ? configurationStore : new ConfigurationStore();

    // For now, the configuration is only obfuscated for Android clients
    requestor =
        new ConfigurationRequestor(
            this.configurationStore, httpClient, expectObfuscatedConfig, supportBandits);
    initialConfigFuture =
        initialConfiguration != null
            ? requestor.setInitialConfiguration(initialConfiguration)
            : null;

    this.assignmentLogger = assignmentLogger;
    this.banditLogger = banditLogger;
    this.isGracefulMode = isGracefulMode;
    // Save SDK name and version to include in logger metadata
    this.sdkName = sdkName;
    this.sdkVersion = sdkVersion;
  }

  private EppoHttpClient buildHttpClient(
      String apiBaseUrl, SDKKey sdkKey, String sdkName, String sdkVersion) {
    ApiEndpoints endpointHelper = new ApiEndpoints(sdkKey, apiBaseUrl);

    return httpClientOverride != null
        ? httpClientOverride
        : new EppoHttpClient(endpointHelper.getBaseUrl(), sdkKey.getToken(), sdkName, sdkVersion);
  }

  protected void loadConfiguration() {
    try {
      requestor.fetchAndSaveFromRemote();
    } catch (Exception ex) {
      log.error("Encountered Exception while loading configuration", ex);
      if (!isGracefulMode) {
        throw ex;
      }
    }
  }

  protected void stopPolling() {
    if (pollTimer != null) {
      pollTimer.cancel();
    }
  }

  /** Start polling using the default interval and jitter. */
  protected void startPolling() {
    startPolling(DEFAULT_POLLING_INTERVAL_MILLIS);
  }

  /**
   * Start polling using the provided polling interval and default jitter of 10%
   *
   * @param pollingIntervalMs The base number of milliseconds to wait between configuration fetches.
   */
  protected void startPolling(long pollingIntervalMs) {
    startPolling(pollingIntervalMs, pollingIntervalMs / DEFAULT_JITTER_INTERVAL_RATIO);
  }

  /**
   * Start polling using the provided interval and jitter.
   *
   * @param pollingIntervalMs The base number of milliseconds to wait between configuration fetches.
   * @param pollingJitterMs The max number of milliseconds to offset each polling interval. The SDK
   *     selects a random number between 0 and pollingJitterMS to offset the polling interval by.
   */
  protected void startPolling(long pollingIntervalMs, long pollingJitterMs) {
    stopPolling();
    log.debug("Started polling at " + pollingIntervalMs + "," + pollingJitterMs);

    // Set up polling for UFC
    pollTimer = new Timer(true);
    FetchConfigurationTask fetchConfigurationsTask =
        new FetchConfigurationTask(
            () -> {
              log.debug("[Eppo SDK] Polling callback");
              this.loadConfiguration();
            },
            pollTimer,
            pollingIntervalMs,
            pollingJitterMs);

    // We don't want to fetch right away, so we schedule the next fetch.
    // Graceful mode is implicit here because `FetchConfigurationsTask` catches and
    // logs errors without rethrowing.
    fetchConfigurationsTask.scheduleNext();
  }

  protected CompletableFuture<Void> loadConfigurationAsync() {
    CompletableFuture<Void> future = new CompletableFuture<>();

    requestor
        .fetchAndSaveFromRemoteAsync()
        .exceptionally(
            ex -> {
              log.error("Encountered Exception while loading configuration", ex);
              if (!isGracefulMode) {
                future.completeExceptionally(ex);
              }
              return null;
            })
        .thenAccept(future::complete);

    return future;
  }

  @NotNull
  protected ValuedFlagEvaluationResult getTypedAssignmentResult(
      @NotNull String flagKey,
      @NotNull String subjectKey,
      @NotNull Attributes subjectAttributes,
      @NotNull EppoValue defaultValue,
      @NotNull VariationType expectedType) {
    throwIfEmptyOrNull(flagKey, "flagKey must not be empty");
    throwIfEmptyOrNull(subjectKey, "subjectKey must not be empty");
    throwIfNull(subjectAttributes, "subjectAttributes must not be empty");
    throwIfNull(defaultValue, "defaultValue must not be empty");
    throwIfNull(expectedType, "expectedType must not be empty");

    @NotNull final Configuration config = getConfiguration();

    @Nullable final FlagConfig maybeFlag = config.getFlag(flagKey);
    if (maybeFlag == null) {
      log.warn("no configuration found for key: {}", flagKey);
      return new ValuedFlagEvaluationResult(defaultValue, null, NO_FLAG_CONFIG);
    }

    @NotNull final FlagConfig flag = maybeFlag;

    if (!flag.isEnabled()) {
      log.info(
          "no assigned variation because the experiment or feature flag is disabled: {}", flagKey);
      return new ValuedFlagEvaluationResult(defaultValue, null, FLAG_DISABLED);
    }

    if (flag.getVariationType() != expectedType) {
      log.warn(
          "no assigned variation because the flag type doesn't match the requested type: {} has type {}, requested {}",
          flagKey,
          flag.getVariationType(),
          expectedType);
      return new ValuedFlagEvaluationResult(defaultValue, null, BAD_VARIATION_TYPE);
    }

    @NotNull final FlagEvaluationResult evaluationResult =
        FlagEvaluator.evaluateFlag(
            flag, flagKey, subjectKey, subjectAttributes, config.isConfigObfuscated());
    @Nullable final FlagEvaluationAllocationKeyAndVariation allocationKeyAndVariation =
        evaluationResult.getAllocationKeyAndVariation();

    @NotNull final ValuedFlagEvaluationResult valuedEvaluationResult;
    if (allocationKeyAndVariation == null) {
      valuedEvaluationResult = new ValuedFlagEvaluationResult(defaultValue, evaluationResult, NO_ALLOCATION);
    } else {
      @NotNull final EppoValue assignedValue = allocationKeyAndVariation.getVariation().getValue();
      if (!valueTypeMatchesExpected(expectedType, assignedValue)) {
        log.warn(
          "no assigned variation because the flag type doesn't match the variation type: {} has type {}, variation value is {}",
          flagKey,
          flag.getVariationType(),
          assignedValue);
         return new ValuedFlagEvaluationResult(defaultValue, evaluationResult, BAD_VALUE_TYPE);
      } else {
        valuedEvaluationResult = new ValuedFlagEvaluationResult(assignedValue, evaluationResult, OK);
        if (assignmentLogger != null && evaluationResult.doLog()) {

          try {
            @NotNull final String allocationKey = allocationKeyAndVariation.getAllocationKey();
            @NotNull final String experimentKey =
                flagKey
                    + '-'
                    + allocationKey; // Our experiment key is derived by hyphenating the flag key and
            // allocation key
            @NotNull final String variationKey = allocationKeyAndVariation.getVariation().getKey();
            @NotNull final Map<String, String> extraLogging = evaluationResult.getExtraLogging();
            @NotNull final Map<String, String> metaData = buildLogMetaData(config.isConfigObfuscated());

            @NotNull final Assignment assignment =
                new Assignment(
                    experimentKey,
                    flagKey,
                    allocationKey,
                    variationKey,
                    subjectKey,
                    subjectAttributes,
                    extraLogging,
                    metaData);
            final boolean logAssignment;
            @NotNull final AssignmentCacheEntry cacheEntry = AssignmentCacheEntry.fromVariationAssignment(assignment);
            if (assignmentCache != null) {
              logAssignment = assignmentCache.putIfAbsent(cacheEntry);
            } else {
              // Deduplication of assignment logging is possible by providing an `IAssignmentCache`.
              // Default to true, only avoid logging if there's a cache hit.
              logAssignment = true;
            }

            if (logAssignment) {
              assignmentLogger.logAssignment(assignment);
            }
          } catch (Exception e) {
            log.error("Error logging assignment: {}", e.getMessage(), e);
          }
        }
      }
    }
    return valuedEvaluationResult;
  }

  @NotNull
  protected EppoValue getTypedAssignment(
      @NotNull String flagKey,
      @NotNull String subjectKey,
      @NotNull Attributes subjectAttributes,
      @NotNull EppoValue defaultValue,
      @NotNull VariationType expectedType) {

    @NotNull final ValuedFlagEvaluationResult valuedEvaluationResult = getTypedAssignmentResult(
        flagKey, subjectKey, subjectAttributes, defaultValue, expectedType);
    return valuedEvaluationResult.getValue();
  }

  private boolean valueTypeMatchesExpected(@NotNull VariationType expectedType, @NotNull EppoValue value) {
    boolean typeMatch;
    switch (expectedType) {
      case BOOLEAN:
        typeMatch = value.isBoolean();
        break;
      case INTEGER:
        typeMatch =
            value.isNumeric()
                // Java has no isInteger check so we check using mod
                && value.doubleValue() % 1 == 0;
        break;
      case NUMERIC:
        typeMatch = value.isNumeric();
        break;
      case STRING:
        typeMatch = value.isString();
        break;
      case JSON:
        typeMatch =
            value.isString()
                // Eppo leaves JSON as a JSON string; to verify it's valid we attempt to parse
                && parseJsonString(value.stringValue()) != null;
        break;
      default:
        throw new IllegalArgumentException("Unexpected type for type checking: " + expectedType);
    }

    return typeMatch;
  }

  public boolean getBooleanAssignment(@NotNull String flagKey, @NotNull String subjectKey, boolean defaultValue) {
    return this.getBooleanAssignment(flagKey, subjectKey, new Attributes(), defaultValue);
  }

  public boolean getBooleanAssignment(
      @NotNull String flagKey, @NotNull String subjectKey, @NotNull Attributes subjectAttributes, boolean defaultValue) {
    try {
      @NotNull final EppoValue value =
          this.getTypedAssignment(
              flagKey,
              subjectKey,
              subjectAttributes,
              EppoValue.valueOf(defaultValue),
              VariationType.BOOLEAN);
      return value.booleanValue();
    } catch (Exception e) {
      return throwIfNotGraceful(e, defaultValue);
    }
  }

  public int getIntegerAssignment(@NotNull String flagKey, @NotNull String subjectKey, int defaultValue) {
    return getIntegerAssignment(flagKey, subjectKey, new Attributes(), defaultValue);
  }

  public int getIntegerAssignment(
      @NotNull String flagKey, @NotNull String subjectKey, @NotNull Attributes subjectAttributes, int defaultValue) {
    try {
      @NotNull final EppoValue value =
          this.getTypedAssignment(
              flagKey,
              subjectKey,
              subjectAttributes,
              EppoValue.valueOf(defaultValue),
              VariationType.INTEGER);
      return Double.valueOf(value.doubleValue()).intValue();
    } catch (Exception e) {
      return throwIfNotGraceful(e, defaultValue);
    }
  }

  @NotNull
  public Double getDoubleAssignment(@NotNull String flagKey, @NotNull String subjectKey, double defaultValue) {
    return getDoubleAssignment(flagKey, subjectKey, new Attributes(), defaultValue);
  }

  @NotNull
  public Double getDoubleAssignment(
      @NotNull String flagKey, @NotNull String subjectKey, @NotNull Attributes subjectAttributes, double defaultValue) {
    try {
      @NotNull final EppoValue value =
          this.getTypedAssignment(
              flagKey,
              subjectKey,
              subjectAttributes,
              EppoValue.valueOf(defaultValue),
              VariationType.NUMERIC);
      return value.doubleValue();
    } catch (Exception e) {
      return throwIfNotGraceful(e, defaultValue);
    }
  }

  @NotNull
  public String getStringAssignment(@NotNull String flagKey, @NotNull String subjectKey, @NotNull String defaultValue) {
    return this.getStringAssignment(flagKey, subjectKey, new Attributes(), defaultValue);
  }

  @NotNull
  public String getStringAssignment(
          @NotNull String flagKey, @NotNull String subjectKey, @NotNull Attributes subjectAttributes, @NotNull String defaultValue) {
    try {
      @NotNull final EppoValue value =
          this.getTypedAssignment(
              flagKey,
              subjectKey,
              subjectAttributes,
              EppoValue.valueOf(defaultValue),
              VariationType.STRING);
      return value.stringValue();
    } catch (Exception e) {
      return throwIfNotGraceful(e, defaultValue);
    }
  }

  /**
   * Returns the assignment for the provided feature flag key and subject key as a {@link JsonNode}.
   * If the flag is not found, does not match the requested type or is disabled, defaultValue is
   * returned.
   *
   * @param flagKey the feature flag key
   * @param subjectKey the subject key
   * @param defaultValue the default value to return if the flag is not found
   * @return the JSON string value of the assignment
   */
  @NotNull
  public JsonNode getJSONAssignment(@NotNull String flagKey, @NotNull String subjectKey, @NotNull JsonNode defaultValue) {
    return getJSONAssignment(flagKey, subjectKey, new Attributes(), defaultValue);
  }

  /**
   * Returns the assignment for the provided feature flag key and subject key as a {@link JsonNode}.
   * If the flag is not found, does not match the requested type or is disabled, defaultValue is
   * returned.
   *
   * @param flagKey the feature flag key
   * @param subjectKey the subject key
   * @param defaultValue the default value to return if the flag is not found
   * @return the JSON string value of the assignment
   */
  @NotNull
  public JsonNode getJSONAssignment(
      @NotNull String flagKey, @NotNull String subjectKey, @NotNull Attributes subjectAttributes, @NotNull JsonNode defaultValue) {
    try {
      @NotNull final EppoValue value =
          this.getTypedAssignment(
              flagKey,
              subjectKey,
              subjectAttributes,
              EppoValue.valueOf(defaultValue.toString()),
              VariationType.JSON);
      @Nullable final JsonNode jsonValue = parseJsonString(value.stringValue());
      return jsonValue != null ? jsonValue : defaultValue;
    } catch (Exception e) {
      return throwIfNotGraceful(e, defaultValue);
    }
  }

  /**
   * Returns the assignment for the provided feature flag key, subject key and subject attributes as
   * a JSON string. If the flag is not found, does not match the requested type or is disabled,
   * defaultValue is returned.
   *
   * @param flagKey the feature flag key
   * @param subjectKey the subject key
   * @param defaultValue the default value to return if the flag is not found
   * @return the JSON string value of the assignment
   */
  @NotNull
  public String getJSONStringAssignment(
      @NotNull String flagKey, @NotNull String subjectKey, @NotNull Attributes subjectAttributes, @NotNull String defaultValue) {
    try {
      @NotNull final EppoValue value =
          this.getTypedAssignment(
              flagKey,
              subjectKey,
              subjectAttributes,
              EppoValue.valueOf(defaultValue),
              VariationType.JSON);
      return value.stringValue();
    } catch (Exception e) {
      return throwIfNotGraceful(e, defaultValue);
    }
  }

  /**
   * Returns the assignment for the provided feature flag key and subject key as a JSON String. If
   * the flag is not found, does not match the requested type or is disabled, defaultValue is
   * returned.
   *
   * @param flagKey the feature flag key
   * @param subjectKey the subject key
   * @param defaultValue the default value to return if the flag is not found
   * @return the JSON string value of the assignment
   */
  @NotNull
  public String getJSONStringAssignment(@NotNull String flagKey, @NotNull String subjectKey, @NotNull String defaultValue) {
    return this.getJSONStringAssignment(flagKey, subjectKey, new Attributes(), defaultValue);
  }

  @Nullable
  private JsonNode parseJsonString(@NotNull String jsonString) {
    try {
      return mapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  public BanditResult getBanditAction(
      String flagKey,
      String subjectKey,
      DiscriminableAttributes subjectAttributes,
      Actions actions,
      String defaultValue) {
    BanditResult result = new BanditResult(defaultValue, null);
    final Configuration config = getConfiguration();
    try {
      String assignedVariation =
          getStringAssignment(
              flagKey, subjectKey, subjectAttributes.getAllAttributes(), defaultValue);

      // Update result to reflect that we've been assigned a variation
      result = new BanditResult(assignedVariation, null);

      String banditKey = config.banditKeyForVariation(flagKey, assignedVariation);
      if (banditKey != null && !actions.isEmpty()) {
        BanditParameters banditParameters = config.getBanditParameters(banditKey);
        BanditEvaluationResult banditResult =
            BanditEvaluator.evaluateBandit(
                flagKey, subjectKey, subjectAttributes, actions, banditParameters.getModelData());

        // Update result to reflect that we've been assigned an action
        result = new BanditResult(assignedVariation, banditResult.getActionKey());

        if (banditLogger != null) {
          try {
            BanditAssignment banditAssignment =
                new BanditAssignment(
                    flagKey,
                    banditKey,
                    subjectKey,
                    banditResult.getActionKey(),
                    banditResult.getActionWeight(),
                    banditResult.getOptimalityGap(),
                    banditParameters.getModelVersion(),
                    subjectAttributes.getNumericAttributes(),
                    subjectAttributes.getCategoricalAttributes(),
                    banditResult.getActionAttributes().getNumericAttributes(),
                    banditResult.getActionAttributes().getCategoricalAttributes(),
                    buildLogMetaData(config.isConfigObfuscated()));

            // Log, only if there is no cache hit.
            boolean logBanditAssignment = true;
            AssignmentCacheEntry cacheEntry =
                AssignmentCacheEntry.fromBanditAssignment(banditAssignment);
            if (banditAssignmentCache != null) {
              if (banditAssignmentCache.hasEntry(cacheEntry)) {
                logBanditAssignment = false;
              }
            }

            if (logBanditAssignment) {
              banditLogger.logBanditAssignment(banditAssignment);

              if (banditAssignmentCache != null) {
                banditAssignmentCache.put(cacheEntry);
              }
            }
          } catch (Exception e) {
            log.warn("Error logging bandit assignment: {}", e.getMessage(), e);
          }
        }
      }
      return result;
    } catch (Exception e) {
      return throwIfNotGraceful(e, result);
    }
  }

  @NotNull
  private Map<String, String> buildLogMetaData(boolean isConfigObfuscated) {
    @NotNull final HashMap<String, String> metaData = new HashMap<>();
    metaData.put("obfuscated", Boolean.valueOf(isConfigObfuscated).toString());
    metaData.put("sdkLanguage", sdkName);
    metaData.put("sdkLibVersion", sdkVersion);
    return metaData;
  }

  @NotNull
  private <T> T throwIfNotGraceful(@NotNull Exception e, @NotNull T defaultValue) {
    if (this.isGracefulMode) {
      log.info("error getting assignment value: {}", e.getMessage());
      return defaultValue;
    }
    throw new RuntimeException(e);
  }

  public void setIsGracefulFailureMode(boolean isGracefulFailureMode) {
    this.isGracefulMode = isGracefulFailureMode;
  }

  /**
   * Subscribe to changes to the configuration.
   *
   * @param callback A function to be executed when the configuration changes.
   * @return a Runnable which, when called unsubscribes the callback from configuration change
   *     events.
   */
  public Runnable onConfigurationChange(Consumer<Configuration> callback) {
    return requestor.onConfigurationChange(callback);
  }

  /**
   * Returns the configuration object used by the EppoClient for assignment and bandit evaluation.
   *
   * <p>The configuration object is for debugging (inspect the loaded config) and other advanced use
   * cases where flag metadata or a list of flag keys, for example, is required.
   *
   * <p>It is not recommended to use the list of keys to preload assignments as assignment
   * computation also logs its use which will affect your metrics.
   *
   * @see <a href="https://docs.geteppo.com/sdks/best-practices/where-to-assign/">Where To
   *     Assign</a> for more details.
   */
  public Configuration getConfiguration() {
    return configurationStore.getConfiguration();
  }
}

package cloud.eppo;

import static cloud.eppo.Utils.getMD5Hex;
import static cloud.eppo.Utils.throwIfEmptyOrNull;

import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.ufc.dto.EppoValue;
import cloud.eppo.ufc.dto.FlagConfig;
import cloud.eppo.ufc.dto.SubjectAttributes;
import cloud.eppo.ufc.dto.VariationType;
import java.util.HashMap;
import java.util.Map;

import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EppoClient {
  private static final Logger log = LoggerFactory.getLogger(EppoClient.class);
  private final ObjectMapper mapper = new ObjectMapper().registerModule(EppoModule.eppoModule()); // TODO: is this the best place for this?
  private static final String DEFAULT_HOST = "https://fscdn.eppo.cloud";
  private static final boolean DEFAULT_IS_GRACEFUL_MODE = true;

  private final ConfigurationRequestor requestor;
  private final AssignmentLogger assignmentLogger;
  private final boolean isGracefulMode;
  private final String sdkName;
  private final String sdkVersion;
  private final boolean isConfigObfuscated;

  private static EppoClient instance;

  // Fields useful for testing in situations where we want to mock the http client or configuration
  // store (accessed via reflection)
  /**
   * @noinspection FieldMayBeFinal
   */
  private static EppoHttpClient httpClientOverride = null;

  /**
   * @noinspection FieldMayBeFinal
   */
  private static ConfigurationStore configurationStoreOverride = null;

  private EppoClient(
      String apiKey,
      String sdkName,
      String sdkVersion,
      String host,
      AssignmentLogger assignmentLogger,
      boolean isGracefulMode
  ) {
    ConfigurationStore configStore =
        configurationStoreOverride == null
            ? new ConfigurationStore()
            : configurationStoreOverride;

    EppoHttpClient httpClient = buildHttpClient(host, apiKey, sdkName, sdkVersion);
    requestor = new ConfigurationRequestor(configStore, httpClient);
    this.assignmentLogger = assignmentLogger;
    this.isGracefulMode = isGracefulMode;
    // Save SDK name and version to include in logger metadata
    this.sdkName = sdkName;
    this.sdkVersion = sdkVersion;
    // For now, the configuration is only obfuscated for Android clients
    this.isConfigObfuscated = sdkName.toLowerCase().contains("android");
    // TODO: caching initialization (such as setting an API-key-specific prefix
  }

  private EppoHttpClient buildHttpClient(String host, String apiKey, String sdkName, String sdkVersion) {
    EppoHttpClient httpClient;
    if (httpClientOverride != null) {
      // Test/Debug - Client is mocked entirely
      httpClient = httpClientOverride;
    } else {
      // Normal operation
      httpClient = new EppoHttpClient(host, apiKey, sdkName, sdkVersion);
    }
    return httpClient;
  }

  public static EppoClient init(
      String apiKey,
      String sdkName,
      String sdkVersion,
      String host,
      AssignmentLogger assignmentLogger,
      boolean isGracefulMode
  ) {

    if (apiKey == null) {
      throw new IllegalArgumentException("Unable to initialize Eppo SDK due to missing API key");
    }
    if (sdkName == null || sdkVersion == null) {
      throw new IllegalArgumentException("Unable to initialize Eppo SDK due to missing SDK name or version");
    }

    if (instance != null) {
      // TODO: also check we're not running a test
      log.warn("Reinitializing an Eppo Client instance that was already initialized");
    }
    instance = new EppoClient(apiKey, sdkName, sdkVersion, host, assignmentLogger, isGracefulMode);
    instance.refreshConfiguration();

    return instance;
  }

  /**
   * Ability to ad-hoc kick off a configuration load. Will load from a filesystem cached file as
   * well as fire off an HTTPS request for an updated configuration. If the cache load finishes
   * first, those assignments will be used until the fetch completes.
   *
   * <p>Deprecated, as we plan to make a more targeted and configurable way to do so in the future.
   */
  @Deprecated
  public void refreshConfiguration() {
    requestor.load();
  }

  // TODO: async way to refresh for android

  protected EppoValue getTypedAssignment(
      String flagKey,
      String subjectKey,
      SubjectAttributes subjectAttributes,
      EppoValue defaultValue,
      VariationType expectedType) {

    throwIfEmptyOrNull(flagKey, "flagKey must not be empty");
    throwIfEmptyOrNull(subjectKey, "subjectKey must not be empty");

    String flagKeyForLookup = flagKey;
    if (isConfigObfuscated) {
      flagKeyForLookup = getMD5Hex(flagKey);
    }

    FlagConfig flag = requestor.getConfiguration(flagKeyForLookup);
    if (flag == null) {
      log.warn("no configuration found for key: " + flagKey);
      return defaultValue;
    }

    if (!flag.isEnabled()) {
      log.info("no assigned variation because the experiment or feature flag is disabled: " + flagKey);
      return defaultValue;
    }

    if (flag.getVariationType() != expectedType) {
      log.warn(
          "no assigned variation because the flag type doesn't match the requested type: "
              + flagKey
              + " has type "
              + flag.getVariationType()
              + ", requested "
              + expectedType);
      return defaultValue;
    }

    FlagEvaluationResult evaluationResult =
        FlagEvaluator.evaluateFlag(
            flag, flagKey, subjectKey, subjectAttributes, isConfigObfuscated);
    EppoValue assignedValue =
        evaluationResult.getVariation() != null ? evaluationResult.getVariation().getValue() : null;

    if (assignedValue != null && !valueTypeMatchesExpected(expectedType, assignedValue)) {
      log.warn(
          "no assigned variation because the flag type doesn't match the variation type: "
              + flagKey
              + " has type "
              + flag.getVariationType()
              + ", variation value is "
              + assignedValue);
      return defaultValue;
    }

    if (assignedValue != null && assignmentLogger != null && evaluationResult.doLog()) {
      String allocationKey = evaluationResult.getAllocationKey();
      String experimentKey =
          flagKey
              + '-'
              + allocationKey; // Our experiment key is derived by hyphenating the flag key and
      // allocation key
      String variationKey = evaluationResult.getVariation().getKey();
      Map<String, String> extraLogging = evaluationResult.getExtraLogging();
      Map<String, String> metaData = new HashMap<>();
      metaData.put("obfuscated", Boolean.valueOf(isConfigObfuscated).toString());
      metaData.put("sdkLanguage", sdkName);
      metaData.put("sdkLibVersion", sdkVersion);

      Assignment assignment =
          Assignment.createWithCurrentDate(
              experimentKey,
              flagKey,
              allocationKey,
              variationKey,
              subjectKey,
              subjectAttributes,
              extraLogging,
              metaData);
      try {
        assignmentLogger.logAssignment(assignment);
      } catch (Exception e) {
        log.warn("Error logging assignment: " + e.getMessage(), e);
      }
    }

    return assignedValue != null ? assignedValue : defaultValue;
  }

  private boolean valueTypeMatchesExpected(VariationType expectedType, EppoValue value) {
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

  public boolean getBooleanAssignment(String flagKey, String subjectKey, boolean defaultValue) {
    return this.getBooleanAssignment(flagKey, subjectKey, new SubjectAttributes(), defaultValue);
  }

  public boolean getBooleanAssignment(
      String flagKey,
      String subjectKey,
      SubjectAttributes subjectAttributes,
      boolean defaultValue) {
    try {
      EppoValue value =
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

  public int getIntegerAssignment(String flagKey, String subjectKey, int defaultValue) {
    return getIntegerAssignment(flagKey, subjectKey, new SubjectAttributes(), defaultValue);
  }

  public int getIntegerAssignment(
      String flagKey, String subjectKey, SubjectAttributes subjectAttributes, int defaultValue) {
    try {
      EppoValue value =
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

  public Double getDoubleAssignment(String flagKey, String subjectKey, double defaultValue) {
    return getDoubleAssignment(flagKey, subjectKey, new SubjectAttributes(), defaultValue);
  }

  public Double getDoubleAssignment(
      String flagKey, String subjectKey, SubjectAttributes subjectAttributes, double defaultValue) {
    try {
      EppoValue value =
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

  public String getStringAssignment(String flagKey, String subjectKey, String defaultValue) {
    return this.getStringAssignment(flagKey, subjectKey, new SubjectAttributes(), defaultValue);
  }

  public String getStringAssignment(
      String flagKey, String subjectKey, SubjectAttributes subjectAttributes, String defaultValue) {
    try {
      EppoValue value =
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
   * Returns the assignment for the provided feature flag key and subject key as a {@link
   * JsonNode}. If the flag is not found, does not match the requested type or is disabled,
   * defaultValue is returned.
   *
   * @param flagKey the feature flag key
   * @param subjectKey the subject key
   * @param defaultValue the default value to return if the flag is not found
   * @return the JSON string value of the assignment
   */
  public JsonNode getJSONAssignment(String flagKey, String subjectKey, JsonNode defaultValue) {
    return getJSONAssignment(flagKey, subjectKey, new SubjectAttributes(), defaultValue);
  }

  /**
   * Returns the assignment for the provided feature flag key and subject key as a {@link
   * JsonNode}. If the flag is not found, does not match the requested type or is disabled,
   * defaultValue is returned.
   *
   * @param flagKey the feature flag key
   * @param subjectKey the subject key
   * @param defaultValue the default value to return if the flag is not found
   * @return the JSON string value of the assignment
   */
  public JsonNode getJSONAssignment(
      String flagKey,
      String subjectKey,
      SubjectAttributes subjectAttributes,
      JsonNode defaultValue) {
    try {
      EppoValue value =
          this.getTypedAssignment(
              flagKey,
              subjectKey,
              subjectAttributes,
              EppoValue.valueOf(defaultValue.toString()),
              VariationType.JSON);
      return parseJsonString(value.stringValue());
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
  public String getJSONStringAssignment(
      String flagKey, String subjectKey, SubjectAttributes subjectAttributes, String defaultValue) {
    try {
      EppoValue value =
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
  public String getJSONStringAssignment(String flagKey, String subjectKey, String defaultValue) {
    return this.getJSONStringAssignment(flagKey, subjectKey, new SubjectAttributes(), defaultValue);
  }

  private JsonNode parseJsonString(String jsonString) {
    try {
      return mapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private <T> T throwIfNotGraceful(Exception e, T defaultValue) {
    if (this.isGracefulMode) {
      log.info("error getting assignment value: " + e.getMessage());
      return defaultValue;
    }
    throw new RuntimeException(e);
  }

  public static EppoClient getInstance() {
    if (EppoClient.instance == null) {
      throw new IllegalStateException("Eppo SDK has not been initialized");
    }

    return EppoClient.instance;
  }

  public static class Builder {
    private String apiKey;
    private String sdkName;
    private String sdkVersion;
    private String host = DEFAULT_HOST;
    private AssignmentLogger assignmentLogger;
    private boolean isGracefulMode = DEFAULT_IS_GRACEFUL_MODE;

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder sdkName(String sdkName) {
      this.sdkName = sdkName;
      return this;
    }

    public Builder sdkVersion(String sdkVersion) {
      this.sdkVersion = sdkVersion;
      return this;
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder assignmentLogger(AssignmentLogger assignmentLogger) {
      this.assignmentLogger = assignmentLogger;
      return this;
    }

    public Builder isGracefulMode(boolean isGracefulMode) {
      this.isGracefulMode = isGracefulMode;
      return this;
    }

    public EppoClient buildAndInit() {
      return EppoClient.init(apiKey, sdkName, sdkVersion, host, assignmentLogger, isGracefulMode);
    }
  }
}
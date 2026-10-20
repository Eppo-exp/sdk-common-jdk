package cloud.eppo;

import static cloud.eppo.helpers.TestUtils.mockConfigurationClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cloud.eppo.api.*;
import cloud.eppo.api.dto.VariationType;
import cloud.eppo.cache.LRUInMemoryAssignmentCache;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JSONStringAssignmentTest {
  private static final String RAW_JSON = "{ \"message\": \"café\", \"enabled\": true }";
  private final ObjectMapper mapper = new ObjectMapper();
  private final AssignmentLogger logger = mock(AssignmentLogger.class);

  private BaseEppoClient<Configuration, JsonNode> client(
      boolean graceful, IAssignmentCache cache, String rawValue) throws Exception {
    return client(graceful, cache, rawValue, VariationType.JSON);
  }

  private BaseEppoClient<Configuration, JsonNode> client(
      boolean graceful, IAssignmentCache cache, String rawValue, VariationType type)
      throws Exception {
    ObjectNode config =
        (ObjectNode)
            mapper.readTree(
                "{\"flags\":{\"json-flag\":{\"key\":\"json-flag\",\"enabled\":true,"
                    + "\"variationType\":\"JSON\",\"variations\":{\"value\":{\"key\":\"value\"}},"
                    + "\"allocations\":[{\"key\":\"rollout\",\"rules\":[],"
                    + "\"splits\":[{\"variationKey\":\"value\",\"shards\":[]}],\"doLog\":true}],"
                    + "\"totalShards\":10000}}}");
    ((ObjectNode) config.at("/flags/json-flag")).put("variationType", type.name());
    ((ObjectNode) config.at("/flags/json-flag/variations/value")).put("value", rawValue);
    BaseEppoClient<Configuration, JsonNode> client =
        new BaseEppoClient<>(
            "test-key",
            "java",
            "test",
            "http://localhost",
            logger,
            null,
            new MemoryOnlyConfigurationStore(),
            graceful,
            false,
            false,
            null,
            cache,
            null,
            new JacksonConfigurationParser(),
            mockConfigurationClient(config.toString().getBytes(StandardCharsets.UTF_8)));
    client.loadConfiguration();
    return client;
  }

  private EvaluationDetails success() {
    return EvaluationDetails.builder()
        .flagEvaluationCode(FlagEvaluationCode.MATCH)
        .variationKey("value")
        .variationValue(EppoValue.valueOf(RAW_JSON))
        .environmentName("test")
        .configFetchedAt(new Date(123))
        .configPublishedAt(new Date(100))
        .build();
  }

  private String assign(
      BaseEppoClient<Configuration, JsonNode> client,
      int overload,
      String flag,
      String fallback,
      EvaluationDetails expectedDetails) {
    Attributes attributes = new Attributes();
    attributes.put("country", EppoValue.valueOf("CA"));
    if (overload == 0) {
      return client.getJSONStringAssignment(flag, "subject", fallback);
    }
    if (overload == 1) {
      return client.getJSONStringAssignment(flag, "subject", attributes, fallback);
    }
    AssignmentDetails<String> details =
        overload == 2
            ? client.getJSONStringAssignmentDetails(flag, "subject", fallback)
            : client.getJSONStringAssignmentDetails(flag, "subject", attributes, fallback);
    if (expectedDetails != null) {
      assertSame(expectedDetails, details.getEvaluationDetails());
    }
    assertNull(details.getAction());
    // Extract as String: comparing Object values would hide the unchecked generic cast.
    String result = details.getVariation();
    return result;
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void successfulJSONStringOverloadsPreserveRawTextAndDetails(int overload) throws Exception {
    BaseEppoClient<Configuration, JsonNode> client = spy(client(false, null, RAW_JSON));
    EvaluationDetails details = success();
    doReturn(details)
        .when(client)
        .evaluateAndMaybeLog(
            eq("json-flag"), eq("subject"), any(Attributes.class), eq(VariationType.JSON));

    assertEquals(RAW_JSON, assign(client, overload, "json-flag", "fallback", details));
    verify(client, times(1))
        .evaluateAndMaybeLog(
            eq("json-flag"), eq("subject"), any(Attributes.class), eq(VariationType.JSON));
  }

  @Test
  void parsedJSONStillReturnsJsonNode() throws Exception {
    BaseEppoClient<Configuration, JsonNode> client = client(false, null, RAW_JSON);
    assertEquals(mapper.readTree(RAW_JSON), client.getJSONAssignment("json-flag", "subject", null));
    assertEquals(
        mapper.readTree(RAW_JSON),
        client
            .getJSONAssignmentDetails("json-flag", "subject", new Attributes(), null)
            .getVariation());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void realJSONFlagEvaluatesAndLogsOncePerCall(int overload) throws Exception {
    BaseEppoClient<Configuration, JsonNode> client = client(false, null, RAW_JSON);
    assertEquals(RAW_JSON, assign(client, overload, "json-flag", null, null));
    verify(logger, times(1)).logAssignment(any(Assignment.class));
  }

  @Test
  void JSONStringLoggingStillDeduplicates() throws Exception {
    BaseEppoClient<Configuration, JsonNode> client =
        client(false, new LRUInMemoryAssignmentCache(10), RAW_JSON);
    assertEquals(RAW_JSON, client.getJSONStringAssignment("json-flag", "subject", null));
    AssignmentDetails<String> details =
        client.getJSONStringAssignmentDetails("json-flag", "subject", null);
    assertEquals(RAW_JSON, details.getVariation());
    assertEquals(FlagEvaluationCode.MATCH, details.getEvaluationDetails().getFlagEvaluationCode());
    verify(logger, times(1)).logAssignment(any(Assignment.class));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void missingFlagPreservesDefaultIncludingNull(int overload) throws Exception {
    BaseEppoClient<Configuration, JsonNode> client = client(false, null, RAW_JSON);
    assertEquals("fallback", assign(client, overload, "missing", "fallback", null));
    assertNull(assign(client, overload, "missing", null, null));
    assertEquals(
        FlagEvaluationCode.FLAG_UNRECOGNIZED_OR_DISABLED,
        client
            .getJSONStringAssignmentDetails("missing", "subject", null)
            .getEvaluationDetails()
            .getFlagEvaluationCode());
    verifyNoInteractions(logger);
  }

  @Test
  void stringFlagIsStillATypeMismatch() throws Exception {
    BaseEppoClient<Configuration, JsonNode> client =
        client(false, null, RAW_JSON, VariationType.STRING);
    AssignmentDetails<String> details =
        client.getJSONStringAssignmentDetails("json-flag", "subject", "fallback");
    assertEquals("fallback", details.getVariation());
    assertEquals(
        FlagEvaluationCode.TYPE_MISMATCH, details.getEvaluationDetails().getFlagEvaluationCode());
    verifyNoInteractions(logger);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void gracefulModePreservesErrorDefaults(int overload) throws Exception {
    BaseEppoClient<Configuration, JsonNode> client = client(true, null, RAW_JSON);
    assertEquals("fallback", assign(client, overload, "", "fallback", null));
    assertNull(assign(client, overload, "", null, null));
    assertEquals(
        FlagEvaluationCode.ASSIGNMENT_ERROR,
        client
            .getJSONStringAssignmentDetails("", "subject", null)
            .getEvaluationDetails()
            .getFlagEvaluationCode());
    verifyNoInteractions(logger);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void strictModeStillThrows(int overload) throws Exception {
    BaseEppoClient<Configuration, JsonNode> client = client(false, null, RAW_JSON);
    assertThrows(RuntimeException.class, () -> assign(client, overload, "", null, null));
    verifyNoInteractions(logger);
  }

  @Test
  void malformedJSONRetainsExistingEvaluationValidation() throws Exception {
    String raw = "{ not valid JSON";
    BaseEppoClient<Configuration, JsonNode> client = client(false, null, raw);
    assertThrows(
        RuntimeException.class, () -> client.getJSONStringAssignment("json-flag", "subject", null));
    BaseEppoClient<Configuration, JsonNode> gracefulClient = client(true, null, raw);
    AssignmentDetails<String> details =
        gracefulClient.getJSONStringAssignmentDetails("json-flag", "subject", "fallback");
    assertEquals("fallback", details.getVariation());
    assertEquals(
        FlagEvaluationCode.ASSIGNMENT_ERROR,
        details.getEvaluationDetails().getFlagEvaluationCode());
    assertThrows(
        RuntimeException.class, () -> client.getJSONAssignment("json-flag", "subject", null));
  }
}

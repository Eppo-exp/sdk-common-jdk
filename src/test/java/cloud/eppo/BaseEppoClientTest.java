package cloud.eppo;

import static cloud.eppo.helpers.AssignmentTestCase.parseTestCaseFile;
import static cloud.eppo.helpers.AssignmentTestCase.runTestCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import cloud.eppo.helpers.AssignmentTestCase;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.ufc.dto.Attributes;
import cloud.eppo.ufc.dto.EppoValue;
import cloud.eppo.ufc.dto.VariationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseEppoClientTest {
  private static final Logger log = LoggerFactory.getLogger(BaseEppoClientTest.class);
  private static final String DUMMY_FLAG_API_KEY = "dummy-flags-api-key"; // Will load flags-v1
  private static final String TEST_HOST =
      "https://us-central1-eppo-qa.cloudfunctions.net/serveGitHubRacTestFile";
  private final ObjectMapper mapper = new ObjectMapper().registerModule(AssignmentTestCase.assignmentTestCaseModule());

  private AssignmentLogger mockAssignmentLogger;

  // TODO: async init client tests

  private void initClient() {
    initClient(TEST_HOST, false, false, DUMMY_FLAG_API_KEY);
  }

  private void initClient(
      String host, boolean isGracefulMode, boolean isConfigObfuscated, String apiKey) {
    mockAssignmentLogger = mock(AssignmentLogger.class);

    new BaseEppoClient.Builder()
        .apiKey(apiKey)
        .sdkName(isConfigObfuscated ? "android" : "java")
        .sdkVersion("3.0.0")
        .isGracefulMode(isGracefulMode)
        .host(host)
        .assignmentLogger(mockAssignmentLogger)
        .buildAndInit();

    log.info("Test client initialized");
  }

  @BeforeEach
  public void cleanUp() {
    // TODO: Clear any caches
    setHttpClientOverrideField(null);
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  public void testUnobfuscatedAssignments(File testFile) {
    initClient(TEST_HOST, false, false, DUMMY_FLAG_API_KEY);
    AssignmentTestCase testCase = parseTestCaseFile(testFile);
    runTestCase(testCase);
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  public void testObfuscatedAssignments(File testFile) {
    initClient(TEST_HOST, false, true, DUMMY_FLAG_API_KEY);
    AssignmentTestCase testCase = parseTestCaseFile(testFile);
    runTestCase(testCase);
  }

  private static Stream<Arguments> getAssignmentTestData() {
    return AssignmentTestCase.getAssignmentTestData();
  }

  @Test
  public void testErrorGracefulModeOn() throws JsonProcessingException {
    initClient(TEST_HOST, true, false, DUMMY_FLAG_API_KEY);

    BaseEppoClient realClient = BaseEppoClient.getInstance();
    BaseEppoClient spyClient = spy(realClient);
    doThrow(new RuntimeException("Exception thrown by mock"))
        .when(spyClient)
        .getTypedAssignment(
            anyString(),
            anyString(),
            any(Attributes.class),
            any(EppoValue.class),
            any(VariationType.class));

    assertTrue(spyClient.getBooleanAssignment("experiment1", "subject1", true));
    assertFalse(spyClient.getBooleanAssignment("experiment1", "subject1", new Attributes(), false));

    assertEquals(10, spyClient.getIntegerAssignment("experiment1", "subject1", 10));
    assertEquals(0, spyClient.getIntegerAssignment("experiment1", "subject1", new Attributes(), 0));

    assertEquals(1.2345, spyClient.getDoubleAssignment("experiment1", "subject1", 1.2345), 0.0001);
    assertEquals(
        0.0,
        spyClient.getDoubleAssignment("experiment1", "subject1", new Attributes(), 0.0),
        0.0001);

    assertEquals("default", spyClient.getStringAssignment("experiment1", "subject1", "default"));
    assertEquals(
        "", spyClient.getStringAssignment("experiment1", "subject1", new Attributes(), ""));

    assertEquals(
        mapper.readTree("{\"a\": 1, \"b\": false}").toString(),
        spyClient
            .getJSONAssignment(
                "subject1", "experiment1", mapper.readTree("{\"a\": 1, \"b\": false}"))
            .toString());

    assertEquals(
        "{\"a\": 1, \"b\": false}",
        spyClient.getJSONStringAssignment("subject1", "experiment1", "{\"a\": 1, \"b\": false}"));

    assertEquals(
        mapper.readTree("{}").toString(),
        spyClient
            .getJSONAssignment("subject1", "experiment1", new Attributes(), mapper.readTree("{}"))
            .toString());
  }

  @Test
  public void testErrorGracefulModeOff() {
    initClient(TEST_HOST, false, false, DUMMY_FLAG_API_KEY);

    BaseEppoClient realClient = BaseEppoClient.getInstance();
    BaseEppoClient spyClient = spy(realClient);
    doThrow(new RuntimeException("Exception thrown by mock"))
        .when(spyClient)
        .getTypedAssignment(
            anyString(),
            anyString(),
            any(Attributes.class),
            any(EppoValue.class),
            any(VariationType.class));

    assertThrows(
        RuntimeException.class,
        () -> spyClient.getBooleanAssignment("experiment1", "subject1", true));
    assertThrows(
        RuntimeException.class,
        () -> spyClient.getBooleanAssignment("experiment1", "subject1", new Attributes(), false));

    assertThrows(
        RuntimeException.class,
        () -> spyClient.getIntegerAssignment("experiment1", "subject1", 10));
    assertThrows(
        RuntimeException.class,
        () -> spyClient.getIntegerAssignment("experiment1", "subject1", new Attributes(), 0));

    assertThrows(
        RuntimeException.class,
        () -> spyClient.getDoubleAssignment("experiment1", "subject1", 1.2345));
    assertThrows(
        RuntimeException.class,
        () -> spyClient.getDoubleAssignment("experiment1", "subject1", new Attributes(), 0.0));

    assertThrows(
        RuntimeException.class,
        () -> spyClient.getStringAssignment("experiment1", "subject1", "default"));
    assertThrows(
        RuntimeException.class,
        () -> spyClient.getStringAssignment("experiment1", "subject1", new Attributes(), ""));

    assertThrows(
        RuntimeException.class,
        () ->
            spyClient.getJSONAssignment(
                "subject1", "experiment1", mapper.readTree("{\"a\": 1, \"b\": false}")));
    assertThrows(
        RuntimeException.class,
        () ->
            spyClient.getJSONAssignment(
                "subject1", "experiment1", new Attributes(), mapper.readTree("{}")));
  }

  @Test
  public void testInvalidConfigJSON() {

    mockHttpResponse("{}");

    initClient(TEST_HOST, false, false, DUMMY_FLAG_API_KEY);

    String result =
        BaseEppoClient.getInstance()
            .getStringAssignment("dummy subject", "dummy flag", "not-populated");
    assertEquals("not-populated", result);
  }

  @Test
  public void testAssignmentEventCorrectlyCreated() {
    Date testStart = new Date();
    initClient();
    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("age", EppoValue.valueOf(30));
    subjectAttributes.put("employer", EppoValue.valueOf("Eppo"));
    double assignment =
        BaseEppoClient.getInstance()
            .getDoubleAssignment("numeric_flag", "alice", subjectAttributes, 0.0);

    assertEquals(3.1415926, assignment, 0.0000001);

    ArgumentCaptor<Assignment> assignmentLogCaptor = ArgumentCaptor.forClass(Assignment.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    Assignment capturedAssignment = assignmentLogCaptor.getValue();
    assertEquals("numeric_flag-rollout", capturedAssignment.getExperiment());
    assertEquals("numeric_flag", capturedAssignment.getFeatureFlag());
    assertEquals("rollout", capturedAssignment.getAllocation());
    assertEquals(
        "pi",
        capturedAssignment
            .getVariation()); // Note: unlike this test, typically variation keys will just be the
    // value for everything not JSON
    assertEquals("alice", capturedAssignment.getSubject());
    assertEquals(subjectAttributes, capturedAssignment.getSubjectAttributes());
    assertEquals(new HashMap<>(), capturedAssignment.getExtraLogging());
    assertTrue(capturedAssignment.getTimestamp().after(testStart));
    Date inTheNearFuture = new Date(System.currentTimeMillis() + 1);
    assertTrue(capturedAssignment.getTimestamp().before(inTheNearFuture));

    Map<String, String> expectedMeta = new HashMap<>();
    expectedMeta.put("obfuscated", "false");
    expectedMeta.put("sdkLanguage", "java");
    expectedMeta.put("sdkLibVersion", "3.0.0");

    assertEquals(expectedMeta, capturedAssignment.getMetaData());
  }

  @Test
  public void testAssignmentLogErrorNonFatal() {
    initClient();
    doThrow(new RuntimeException("Mock Assignment Logging Error"))
        .when(mockAssignmentLogger)
        .logAssignment(any());
    double assignment =
        BaseEppoClient.getInstance()
            .getDoubleAssignment("numeric_flag", "alice", new Attributes(), 0.0);

    assertEquals(3.1415926, assignment, 0.0000001);

    ArgumentCaptor<Assignment> assignmentLogCaptor = ArgumentCaptor.forClass(Assignment.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
  }

  private void mockHttpResponse(String responseBody) {
    // Create a mock instance of EppoHttpClient
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    // Mock sync get
    Response dummyResponse =
        new Response.Builder()
            // Used by test
            .code(200)
            .body(ResponseBody.create(responseBody, MediaType.get("application/json")))
            // Below properties are required to build the Response (but unused)
            .request(new Request.Builder().url(TEST_HOST).build())
            .protocol(Protocol.HTTP_1_1)
            .message("OK")
            .build();
    when(mockHttpClient.get(anyString())).thenReturn(dummyResponse);

    // Mock async get
    doAnswer(
            invocation -> {
              RequestCallback callback = invocation.getArgument(1);
              callback.onSuccess(responseBody);
              return null; // doAnswer doesn't require a return value
            })
        .when(mockHttpClient)
        .get(anyString(), any(RequestCallback.class));

    setHttpClientOverrideField(mockHttpClient);
  }

  private void setHttpClientOverrideField(EppoHttpClient httpClient) {
    setOverrideField("httpClientOverride", httpClient);
  }

  private void setConfigurationStoreOverrideField(ConfigurationStore configurationStore) {
    setOverrideField("configurationStoreOverride", configurationStore);
  }

  /** Uses reflection to set a static override field used for tests (e.g., httpClientOverride) */
  private <T> void setOverrideField(String fieldName, T override) {
    try {
      Field httpClientOverrideField = BaseEppoClient.class.getDeclaredField(fieldName);
      httpClientOverrideField.setAccessible(true);
      httpClientOverrideField.set(null, override);
      httpClientOverrideField.setAccessible(false);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO: tests for the cache
}

package cloud.eppo;

import static cloud.eppo.helpers.AssignmentTestCase.parseTestCaseFile;
import static cloud.eppo.helpers.AssignmentTestCase.runTestCase;
import static cloud.eppo.helpers.TestUtils.mockHttpResponse;
import static cloud.eppo.helpers.TestUtils.setBaseClientHttpClientOverrideField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import cloud.eppo.api.Attributes;
import cloud.eppo.api.Configuration;
import cloud.eppo.api.EppoValue;
import cloud.eppo.helpers.AssignmentTestCase;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.ufc.dto.VariationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
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
  private final ObjectMapper mapper =
      new ObjectMapper().registerModule(AssignmentTestCase.assignmentTestCaseModule());

  private BaseEppoClient eppoClient;
  private AssignmentLogger mockAssignmentLogger;

  private File initialFlagConfigFile =
      new File("src/test/resources/static/initial-flag-config.json");

  // TODO: async init client tests

  private void initClient() {
    initClient(false, false);
  }

  private void initClientWithData(
      final String initialFlagConfiguration, boolean isGracefulMode, boolean isConfigObfuscated) {
    mockAssignmentLogger = mock(AssignmentLogger.class);

    Configuration initialConfig =
        new Configuration.Builder(initialFlagConfiguration, isConfigObfuscated).build();

    eppoClient =
        new BaseEppoClient(
            DUMMY_FLAG_API_KEY,
            isConfigObfuscated ? "android" : "java",
            "3.0.0",
            TEST_HOST,
            mockAssignmentLogger,
            null,
            isGracefulMode,
            isConfigObfuscated,
            initialConfig);
  }

  private void initClient(boolean isGracefulMode, boolean isConfigObfuscated) {
    mockAssignmentLogger = mock(AssignmentLogger.class);

    eppoClient =
        new BaseEppoClient(
            DUMMY_FLAG_API_KEY,
            isConfigObfuscated ? "android" : "java",
            "3.0.0",
            TEST_HOST,
            mockAssignmentLogger,
            null,
            isGracefulMode,
            isConfigObfuscated);

    eppoClient.loadConfiguration();
    log.info("Test client initialized");
  }

  @BeforeEach
  public void cleanUp() {
    // TODO: Clear any caches
    setBaseClientHttpClientOverrideField(null);
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  public void testUnobfuscatedAssignments(File testFile) {
    initClient(false, false);
    AssignmentTestCase testCase = parseTestCaseFile(testFile);
    runTestCase(testCase, eppoClient);
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  public void testObfuscatedAssignments(File testFile) {
    initClient(false, true);
    AssignmentTestCase testCase = parseTestCaseFile(testFile);
    runTestCase(testCase, eppoClient);
  }

  private static Stream<Arguments> getAssignmentTestData() {
    return AssignmentTestCase.getAssignmentTestData();
  }

  @Test
  public void testErrorGracefulModeOn() throws JsonProcessingException {
    initClient(true, false);

    BaseEppoClient realClient = eppoClient;
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
    initClient(false, false);

    BaseEppoClient realClient = eppoClient;
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

    mockHttpResponse(TEST_HOST, "{}");

    initClient(false, false);

    String result = eppoClient.getStringAssignment("dummy flag", "dummy subject", "not-populated");
    assertEquals("not-populated", result);
  }

  @Test
  public void testInvalidInitialConfigurationHandledGracefully() {
    initClientWithData("{}", true, false);

    String result = eppoClient.getStringAssignment("dummy flag", "dummy subject", "not-populated");
    assertEquals("not-populated", result);
  }

  @Test
  public void testWithInitialConfiguration() {
    try {
      String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, "UTF8");

      initClientWithData(flagConfig, false, false);

      double result = eppoClient.getDoubleAssignment("numeric_flag", "dummy subject", 0);
      assertEquals(5, result);

      // Demonstrate that loaded configuration is different from the initial string passed above.
      eppoClient.loadConfiguration();
      double updatedResult = eppoClient.getDoubleAssignment("numeric_flag", "dummy subject", 0);
      assertEquals(3.1415926, updatedResult);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAssignmentEventCorrectlyCreated() {
    Date testStart = new Date();
    initClient();
    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("age", EppoValue.valueOf(30));
    subjectAttributes.put("employer", EppoValue.valueOf("Eppo"));
    double assignment =
        eppoClient.getDoubleAssignment("numeric_flag", "alice", subjectAttributes, 0.0);

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
        eppoClient.getDoubleAssignment("numeric_flag", "alice", new Attributes(), 0.0);

    assertEquals(3.1415926, assignment, 0.0000001);

    ArgumentCaptor<Assignment> assignmentLogCaptor = ArgumentCaptor.forClass(Assignment.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
  }

  // TODO: tests for the cache
}

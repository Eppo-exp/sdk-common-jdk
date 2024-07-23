package cloud.eppo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cloud.eppo.helpers.AssignmentTestCase;
import cloud.eppo.helpers.AssignmentTestCaseDeserializer;
import cloud.eppo.helpers.SubjectAssignment;
import cloud.eppo.helpers.TestCaseValue;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.ufc.dto.EppoValue;
import cloud.eppo.ufc.dto.SubjectAttributes;
import cloud.eppo.ufc.dto.VariationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

public class EppoClientTest {
  private static final Logger log = LoggerFactory.getLogger(EppoClientTest.class);
  private static final String DUMMY_API_KEY = "mock-api-key";
  private static final String DUMMY_OTHER_API_KEY = "another-mock-api-key";
  private static final String TEST_HOST =
      "https://us-central1-eppo-qa.cloudfunctions.net/serveGitHubRacTestFile";
  private static final String INVALID_HOST = "https://thisisabaddomainforthistest.com";
  private final ObjectMapper mapper = new ObjectMapper().registerModule(module());

  private AssignmentLogger mockAssignmentLogger;

  //TODO: async init client tests

  private void initClient() {
    initClient(TEST_HOST, false, false, DUMMY_API_KEY);
  }

  private void initClient(
      String host,
      boolean isGracefulMode,
      boolean isConfigObfuscated,
      String apiKey) {
    mockAssignmentLogger = mock(AssignmentLogger.class);

    new EppoClient.Builder()
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
    setConfigurationStoreOverrideField(null);
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  public void testUnobfuscatedAssignments(File testFile) {
    initClient(TEST_HOST, false, false, DUMMY_API_KEY);
    AssignmentTestCase testCase = parseTestCaseFile(testFile);
    runTestCase(testCase);
  }

  @ParameterizedTest
  @MethodSource("getAssignmentTestData")
  public void testObfsucatedAssignments(File testFile) {
    initClient(TEST_HOST, false, true, DUMMY_API_KEY);
    AssignmentTestCase testCase = parseTestCaseFile(testFile);
    runTestCase(testCase);
  }

  private static Stream<Arguments> getAssignmentTestData() {
    File testCaseFolder = new File("src/test/resources/shared/ufc/tests");
    File[] testCaseFiles = testCaseFolder.listFiles();
    assertNotNull(testCaseFiles);
    assertTrue(testCaseFiles.length > 0);
    List<Arguments> arguments = new ArrayList<>();
    for (File testCaseFile : testCaseFiles) {
      arguments.add(Arguments.of(testCaseFile));
    }
    return arguments.stream();
  }

  private AssignmentTestCase parseTestCaseFile(File testCaseFile) {
    AssignmentTestCase testCase;
    try {
      String json = FileUtils.readFileToString(testCaseFile, "UTF8");
      testCase = mapper.readValue(json, AssignmentTestCase.class);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return testCase;
  }

  private void runTestCase(AssignmentTestCase testCase) {
    String flagKey = testCase.getFlag();
    TestCaseValue defaultValue = testCase.getDefaultValue();
    EppoClient eppoClient = EppoClient.getInstance();
    assertFalse(testCase.getSubjects().isEmpty());

    for (SubjectAssignment subjectAssignment : testCase.getSubjects()) {
      String subjectKey = subjectAssignment.getSubjectKey();
      SubjectAttributes subjectAttributes = subjectAssignment.getSubjectAttributes();

      // Depending on the variation type, we will need to change which assignment method we call and
      // how we get the default value
      switch (testCase.getVariationType()) {
        case BOOLEAN:
          boolean boolAssignment =
            eppoClient.getBooleanAssignment(
              flagKey, subjectKey, subjectAttributes, defaultValue.booleanValue());
          assertAssignment(flagKey, subjectAssignment, boolAssignment);
          break;
        case INTEGER:
          int intAssignment =
            eppoClient.getIntegerAssignment(
              flagKey,
              subjectKey,
              subjectAttributes,
              Double.valueOf(defaultValue.doubleValue()).intValue());
          assertAssignment(flagKey, subjectAssignment, intAssignment);
          break;
        case NUMERIC:
          double doubleAssignment =
            eppoClient.getDoubleAssignment(
              flagKey, subjectKey, subjectAttributes, defaultValue.doubleValue());
          assertAssignment(flagKey, subjectAssignment, doubleAssignment);
          break;
        case STRING:
          String stringAssignment =
            eppoClient.getStringAssignment(
              flagKey, subjectKey, subjectAttributes, defaultValue.stringValue());
          assertAssignment(flagKey, subjectAssignment, stringAssignment);
          break;
        case JSON:
          JsonNode jsonAssignment =
            eppoClient.getJSONAssignment(
              flagKey,
              subjectKey,
              subjectAttributes,
              testCase.getDefaultValue().jsonValue()
            );
          assertAssignment(flagKey, subjectAssignment, jsonAssignment);
          break;
        default:
          throw new UnsupportedOperationException(
            "Unexpected variation type "
              + testCase.getVariationType()
              + " for "
              + flagKey
              + " test case");
      }
    }
  }

  /** Helper method for asserting a subject assignment with a useful failure message. */
  private <T> void assertAssignment(
    String flagKey, SubjectAssignment expectedSubjectAssignment, T assignment) {

    if (assignment == null) {
      fail(
        "Unexpected null "
          + flagKey
          + " assignment for subject "
          + expectedSubjectAssignment.getSubjectKey());
    }

    String failureMessage =
      "Incorrect "
        + flagKey
        + " assignment for subject "
        + expectedSubjectAssignment.getSubjectKey();

    if (assignment instanceof Boolean) {
      assertEquals(
        failureMessage, expectedSubjectAssignment.getAssignment().booleanValue(), assignment);
    } else if (assignment instanceof Integer) {
      assertEquals(
        failureMessage,
        Double.valueOf(expectedSubjectAssignment.getAssignment().doubleValue()).intValue(),
        assignment);
    } else if (assignment instanceof Double) {
      assertEquals(
        failureMessage,
        expectedSubjectAssignment.getAssignment().doubleValue(),
        (Double) assignment,
        0.000001);
    } else if (assignment instanceof String) {
      assertEquals(
        failureMessage, expectedSubjectAssignment.getAssignment().stringValue(), assignment);
    } else if (assignment instanceof JsonNode) {
      assertEquals(
        failureMessage,
        expectedSubjectAssignment.getAssignment().jsonValue().toString(),
        assignment.toString());
    } else {
      throw new IllegalArgumentException(
        "Unexpected assignment type " + assignment.getClass().getCanonicalName());
    }
  }

  @Test
  public void testErrorGracefulModeOn() throws JsonProcessingException {
    initClient(TEST_HOST, true, false, DUMMY_API_KEY);

    EppoClient realClient = EppoClient.getInstance();
    EppoClient spyClient = spy(realClient);
    doThrow(new RuntimeException("Exception thrown by mock"))
        .when(spyClient)
        .getTypedAssignment(
            anyString(),
            anyString(),
            any(SubjectAttributes.class),
            any(EppoValue.class),
            any(VariationType.class));

    assertTrue(spyClient.getBooleanAssignment("experiment1", "subject1", true));
    assertFalse(
        spyClient.getBooleanAssignment("experiment1", "subject1", new SubjectAttributes(), false));

    assertEquals(10, spyClient.getIntegerAssignment("experiment1", "subject1", 10));
    assertEquals(
        0, spyClient.getIntegerAssignment("experiment1", "subject1", new SubjectAttributes(), 0));

    assertEquals(1.2345, spyClient.getDoubleAssignment("experiment1", "subject1", 1.2345), 0.0001);
    assertEquals(
        0.0,
        spyClient.getDoubleAssignment("experiment1", "subject1", new SubjectAttributes(), 0.0),
        0.0001);

    assertEquals("default", spyClient.getStringAssignment("experiment1", "subject1", "default"));
    assertEquals(
        "", spyClient.getStringAssignment("experiment1", "subject1", new SubjectAttributes(), ""));

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
            .getJSONAssignment(
                "subject1", "experiment1", new SubjectAttributes(), mapper.readTree("{}"))
            .toString());
  }

  @Test
  public void testErrorGracefulModeOff() {
    initClient(TEST_HOST, false, false, DUMMY_API_KEY);

    EppoClient realClient = EppoClient.getInstance();
    EppoClient spyClient = spy(realClient);
    doThrow(new RuntimeException("Exception thrown by mock"))
        .when(spyClient)
        .getTypedAssignment(
            anyString(),
            anyString(),
            any(SubjectAttributes.class),
            any(EppoValue.class),
            any(VariationType.class));

    assertThrows(
        RuntimeException.class,
        () -> spyClient.getBooleanAssignment("experiment1", "subject1", true));
    assertThrows(
        RuntimeException.class,
        () ->
            spyClient.getBooleanAssignment(
                "experiment1", "subject1", new SubjectAttributes(), false));

    assertThrows(
        RuntimeException.class,
        () -> spyClient.getIntegerAssignment("experiment1", "subject1", 10));
    assertThrows(
        RuntimeException.class,
        () ->
            spyClient.getIntegerAssignment("experiment1", "subject1", new SubjectAttributes(), 0));

    assertThrows(
        RuntimeException.class,
        () -> spyClient.getDoubleAssignment("experiment1", "subject1", 1.2345));
    assertThrows(
        RuntimeException.class,
        () ->
            spyClient.getDoubleAssignment("experiment1", "subject1", new SubjectAttributes(), 0.0));

    assertThrows(
        RuntimeException.class,
        () -> spyClient.getStringAssignment("experiment1", "subject1", "default"));
    assertThrows(
        RuntimeException.class,
        () ->
            spyClient.getStringAssignment("experiment1", "subject1", new SubjectAttributes(), ""));

    assertThrows(
        RuntimeException.class,
        () ->
            spyClient.getJSONAssignment(
                "subject1", "experiment1", mapper.readTree("{\"a\": 1, \"b\": false}")));
    assertThrows(
        RuntimeException.class,
        () ->
            spyClient.getJSONAssignment(
                "subject1", "experiment1", new SubjectAttributes(), mapper.readTree("{}")));
  }

  @Test
  public void testInvalidConfigJSON() {

    // Create a mock instance of EppoHttpClient
    EppoHttpClient mockHttpClient = mock(EppoHttpClient.class);

    doAnswer(
            invocation -> {
              RequestCallback callback = invocation.getArgument(1);
              callback.onSuccess("{}");
              return null; // doAnswer doesn't require a return value
            })
        .when(mockHttpClient)
        .get(anyString(), any(RequestCallback.class));

    setHttpClientOverrideField(mockHttpClient);
    assertThrows(RuntimeException.class, () -> initClient(TEST_HOST, true, false, DUMMY_API_KEY));

    String result =
        EppoClient.getInstance()
            .getStringAssignment("dummy subject", "dummy flag", "not-populated");
    assertEquals("not-populated", result);
  }

  @Test
  public void testAssignmentEventCorrectlyCreated() {
    Date testStart = new Date();
    initClient();
    SubjectAttributes subjectAttributes = new SubjectAttributes();
    subjectAttributes.put("age", EppoValue.valueOf(30));
    subjectAttributes.put("employer", EppoValue.valueOf("Eppo"));
    double assignment =
        EppoClient.getInstance()
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

    try {
      Date assertionDate = new Date();
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
      dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
      Date parsedTimestamp = dateFormat.parse(capturedAssignment.getTimestamp());
      assertNotNull(parsedTimestamp);
      assertTrue(parsedTimestamp.after(testStart));
      assertTrue(parsedTimestamp.before(assertionDate));
    } catch (ParseException ex) {
      throw new RuntimeException(ex);
    }

    Map<String, String> expectedMeta = new HashMap<>();
    expectedMeta.put("obfuscated", "false");
    expectedMeta.put("sdkLanguage", "java");
    expectedMeta.put("sdkLibVersion", "3.0.0");

    assertEquals(expectedMeta, capturedAssignment.getMetaData());
  }

  private void setHttpClientOverrideField(EppoHttpClient httpClient) {
    setOverrideField("httpClientOverride", httpClient);
  }

  private void setConfigurationStoreOverrideField(ConfigurationStore configurationStore) {
    setOverrideField("configurationStoreOverride", configurationStore);
  }

  private <T> void setOverrideField(String fieldName, T override) {
    try {
      // Use reflection to set the httpClientOverride field
      Field httpClientOverrideField = EppoClient.class.getDeclaredField(fieldName);
      httpClientOverrideField.setAccessible(true);
      httpClientOverrideField.set(null, override);
      httpClientOverrideField.setAccessible(false);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static SimpleModule module() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(AssignmentTestCase.class, new AssignmentTestCaseDeserializer());
    return module;
  }

  // TODO: tests for the cache
}

package cloud.eppo;

import static cloud.eppo.helpers.AssignmentTestCase.parseTestCaseFile;
import static cloud.eppo.helpers.AssignmentTestCase.runTestCase;
import static cloud.eppo.helpers.TestUtils.mockHttpError;
import static cloud.eppo.helpers.TestUtils.mockHttpResponse;
import static cloud.eppo.helpers.TestUtils.setBaseClientHttpClientOverrideField;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import cloud.eppo.api.*;
import cloud.eppo.cache.LRUInMemoryAssignmentCache;
import cloud.eppo.helpers.AssignmentTestCase;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.ufc.dto.VariationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

  // Use branch if specified by env variable `TEST_DATA_BRANCH`.
  private static final String TEST_BRANCH = System.getenv("TEST_DATA_BRANCH");
  private static final String TEST_HOST_BASE =
      "https://us-central1-eppo-qa.cloudfunctions.net/serveGitHubRacTestFile";
  private static final String TEST_HOST =
      TEST_HOST_BASE + (TEST_BRANCH != null ? "/b/" + TEST_BRANCH : "");

  private final ObjectMapper mapper =
      new ObjectMapper().registerModule(AssignmentTestCase.assignmentTestCaseModule());

  private BaseEppoClient eppoClient;
  private AssignmentLogger mockAssignmentLogger;

  private final File initialFlagConfigFile =
      new File("src/test/resources/static/initial-flag-config.json");

  // TODO: async init client tests

  private void initClient() {
    initClient(false, false);
  }

  private void initClientWithData(
      final CompletableFuture<Configuration> initialFlagConfiguration,
      boolean isConfigObfuscated,
      boolean isGracefulMode) {
    mockAssignmentLogger = mock(AssignmentLogger.class);

    eppoClient =
        new BaseEppoClient(
            DUMMY_FLAG_API_KEY,
            isConfigObfuscated ? "android" : "java",
            "100.1.0",
            TEST_HOST,
            mockAssignmentLogger,
            null,
            null,
            isGracefulMode,
            isConfigObfuscated,
            true,
            initialFlagConfiguration,
            null,
            null);
  }

  private void initClient(boolean isGracefulMode, boolean isConfigObfuscated) {
    mockAssignmentLogger = mock(AssignmentLogger.class);

    eppoClient =
        new BaseEppoClient(
            DUMMY_FLAG_API_KEY,
            isConfigObfuscated ? "android" : "java",
            "100.1.0",
            TEST_HOST,
            mockAssignmentLogger,
            null,
            null,
            isGracefulMode,
            isConfigObfuscated,
            true,
            null,
            null,
            null);

    eppoClient.loadConfiguration();
    log.info("Test client initialized");
  }

  private CompletableFuture<Void> initClientAsync(
      boolean isGracefulMode, boolean isConfigObfuscated) {
    mockAssignmentLogger = mock(AssignmentLogger.class);

    eppoClient =
        new BaseEppoClient(
            DUMMY_FLAG_API_KEY,
            isConfigObfuscated ? "android" : "java",
            "100.1.0",
            TEST_HOST,
            mockAssignmentLogger,
            null,
            null,
            isGracefulMode,
            isConfigObfuscated,
            true,
            null,
            null,
            null);

    return eppoClient.loadConfigurationAsync();
  }

  private void initClientWithAssignmentCache(IAssignmentCache cache) {
    mockAssignmentLogger = mock(AssignmentLogger.class);

    eppoClient =
        new BaseEppoClient(
            DUMMY_FLAG_API_KEY,
            "java",
            "100.1.0",
            TEST_HOST,
            mockAssignmentLogger,
            null,
            null,
            true,
            false,
            true,
            null,
            cache,
            null);

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

  private CompletableFuture<Configuration> immediateConfigFuture(
      String config, boolean isObfuscated) {
    return CompletableFuture.completedFuture(
        Configuration.builder(config.getBytes(), isObfuscated).build());
  }

  @Test
  public void testGracefulInitializationFailure() {
    // Set up bad HTTP response
    mockHttpError();

    // Initialize and no exception should be thrown.
    assertDoesNotThrow(() -> initClient(true, false));
  }

  @Test
  public void testClientMakesDefaultAssignmentsAfterFailingToInitialize() {
    // Set up bad HTTP response
    mockHttpError();

    // Initialize and no exception should be thrown.
    assertDoesNotThrow(() -> initClient(true, false));

    assertEquals("default", eppoClient.getStringAssignment("experiment1", "subject1", "default"));
  }

  @Test
  public void testClientMakesDefaultAssignmentsAfterFailingToInitializeNonGracefulMode() {
    // Set up bad HTTP response
    mockHttpError();

    // Initialize and no exception should be thrown.
    try {
      initClient(false, false);
    } catch (RuntimeException e) {
      // Expected
      assertEquals("Intentional Error", e.getMessage());
    } finally {
      assertEquals("default", eppoClient.getStringAssignment("experiment1", "subject1", "default"));
    }
  }

  @Test
  public void testNonGracefulInitializationFailure() {
    // Set up bad HTTP response
    mockHttpError();

    // Initialize and assert exception thrown
    assertThrows(Exception.class, () -> initClient(false, false));
  }

  @Test
  public void testGracefulAsyncInitializationFailure() {
    // Set up bad HTTP response
    mockHttpError();

    // Initialize
    CompletableFuture<Void> init = initClientAsync(true, false);

    // Wait for initialization; future should not complete exceptionally (equivalent of exception
    // being thrown).
    init.join();
    assertFalse(init.isCompletedExceptionally());
  }

  @Test
  public void testNonGracefulAsyncInitializationFailure() {
    // Set up bad HTTP response
    mockHttpError();

    // Initialize
    CompletableFuture<Void> init = initClientAsync(false, false);

    // Exceptions thrown in CompletableFutures are wrapped in a CompletionException.
    assertThrows(CompletionException.class, init::join);
    assertTrue(init.isCompletedExceptionally());
  }

  @Test
  public void testWithInitialConfiguration() {
    try {
      String flagConfig = FileUtils.readFileToString(initialFlagConfigFile, "UTF8");

      initClientWithData(immediateConfigFuture(flagConfig, false), false, true);

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
  public void testWithInitialConfigurationFuture() throws IOException {
    CompletableFuture<Configuration> futureConfig = new CompletableFuture<>();
    byte[] flagConfig = FileUtils.readFileToByteArray(initialFlagConfigFile);

    initClientWithData(futureConfig, false, true);

    double result = eppoClient.getDoubleAssignment("numeric_flag", "dummy subject", 0);
    assertEquals(0, result);

    // Now, complete the initial config future and check the value.
    futureConfig.complete(Configuration.builder(flagConfig, false).build());

    result = eppoClient.getDoubleAssignment("numeric_flag", "dummy subject", 0);
    assertEquals(5, result);
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
    expectedMeta.put("sdkLibVersion", "100.1.0");

    assertEquals(expectedMeta, capturedAssignment.getMetaData());
  }

  @Test
  public void testAssignmentNotDeduplicatedWithoutCache() {
    initClient();

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("age", EppoValue.valueOf(30));
    subjectAttributes.put("employer", EppoValue.valueOf("Eppo"));

    // Get the assignment twice
    eppoClient.getDoubleAssignment("numeric_flag", "alice", subjectAttributes, 0.0);
    eppoClient.getDoubleAssignment("numeric_flag", "alice", subjectAttributes, 0.0);

    ArgumentCaptor<Assignment> assignmentLogCaptor = ArgumentCaptor.forClass(Assignment.class);

    // `logAssignment` should be called twice;
    verify(mockAssignmentLogger, times(2)).logAssignment(assignmentLogCaptor.capture());
  }

  @Test
  public void testAssignmentEventCorrectlyDeduplicated() {
    initClientWithAssignmentCache(new LRUInMemoryAssignmentCache(1024));

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("number", EppoValue.valueOf("123456789"));

    // Get the assignment twice
    int assignment =
        eppoClient.getIntegerAssignment("numeric-one-of", "alice", subjectAttributes, 0);
    eppoClient.getIntegerAssignment("numeric-one-of", "alice", subjectAttributes, 0);

    // `2` matches the attribute `number` value of "123456789"
    assertEquals(2, assignment);

    // `logAssignment` should be called only once.
    verify(mockAssignmentLogger, times(1)).logAssignment(any(Assignment.class));

    // Now, change the assigned value to get a logged entry. `number="1"` will map to the assignment
    // of `1`.
    subjectAttributes.put("number", EppoValue.valueOf("1"));

    // Get the assignment
    int newAssignment =
        eppoClient.getIntegerAssignment("numeric-one-of", "alice", subjectAttributes, 0);
    assertEquals(1, newAssignment);

    // Verify a new log call
    verify(mockAssignmentLogger, times(2)).logAssignment(any(Assignment.class));

    // Change back to the original variation to ensure it is not still cached after the previous
    // value evicted it.
    subjectAttributes.put("number", EppoValue.valueOf("123456789"));

    // Get the assignment
    int oldAssignment =
        eppoClient.getIntegerAssignment("numeric-one-of", "alice", subjectAttributes, 0);
    assertEquals(2, oldAssignment);

    // Verify a new log call
    verify(mockAssignmentLogger, times(3)).logAssignment(any(Assignment.class));
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
}

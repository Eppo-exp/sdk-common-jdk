package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cloud.eppo.helpers.*;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditAssignment;
import cloud.eppo.logging.BanditLogger;
import cloud.eppo.ufc.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EppoClientBanditTest {
  private static final Logger log = LoggerFactory.getLogger(EppoClientBanditTest.class);
  private static final String DUMMY_BANDIT_API_KEY =
      "dummy-bandits-api-key"; // Will load bandit-flags-v1
  private static final String TEST_HOST =
      "https://us-central1-eppo-qa.cloudfunctions.net/serveGitHubRacTestFile";
  private static final ObjectMapper mapper = new ObjectMapper().registerModule(module());

  private static final AssignmentLogger mockAssignmentLogger = mock(AssignmentLogger.class);
  private static final BanditLogger mockBanditLogger = mock(BanditLogger.class);
  private static final Date testStart = new Date();

  // TODO: possibly consolidate code between this and the non-bandit test

  @BeforeAll
  public static void initClient() {

    new EppoClient.Builder()
        .apiKey(DUMMY_BANDIT_API_KEY)
        .sdkName("java")
        .sdkVersion("3.0.0")
        .isGracefulMode(false)
        .host(TEST_HOST)
        .assignmentLogger(mockAssignmentLogger)
        .banditLogger(mockBanditLogger)
        .buildAndInit();

    log.info("Test client initialized");
  }

  @BeforeEach
  public void reset() {
    clearInvocations(mockAssignmentLogger);
    clearInvocations(mockBanditLogger);
    EppoClient.getInstance().setIsGracefulFailureMode(false);
  }

  @ParameterizedTest
  @MethodSource("getBanditTestData")
  public void testUnobfuscatedBanditAssignments(File testFile) {
    BanditTestCase testCase = parseTestCaseFile(testFile);
    runBanditTestCase(testCase);
  }

  private static Stream<Arguments> getBanditTestData() {
    File testCaseFolder = new File("src/test/resources/shared/ufc/bandit-tests");
    File[] testCaseFiles = testCaseFolder.listFiles();
    assertNotNull(testCaseFiles);
    assertTrue(testCaseFiles.length > 0);
    List<Arguments> arguments = new ArrayList<>();
    for (File testCaseFile : testCaseFiles) {
      arguments.add(Arguments.of(testCaseFile));
    }
    return arguments.stream();
  }

  private BanditTestCase parseTestCaseFile(File testCaseFile) {
    BanditTestCase testCase;
    try {
      String json = FileUtils.readFileToString(testCaseFile, "UTF8");
      testCase = mapper.readValue(json, BanditTestCase.class);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return testCase;
  }

  private void runBanditTestCase(BanditTestCase testCase) {
    assertFalse(testCase.getSubjects().isEmpty());

    String flagKey = testCase.getFlag();
    String defaultValue = testCase.getDefaultValue();

    for (BanditSubjectAssignment subjectAssignment : testCase.getSubjects()) {
      String subjectKey = subjectAssignment.getSubjectKey();
      ContextAttributes attributes = subjectAssignment.getSubjectAttributes();
      Actions actions = subjectAssignment.getActions();
      BanditResult assignment =
          EppoClient.getInstance()
              .getBanditAction(flagKey, subjectKey, attributes, actions, defaultValue);
      assertBanditAssignment(flagKey, subjectAssignment, assignment);
    }
  }

  /** Helper method for asserting a bandit assignment with a useful failure message. */
  private void assertBanditAssignment(
      String flagKey, BanditSubjectAssignment expectedSubjectAssignment, BanditResult assignment) {
    String failureMessage =
        "Incorrect "
            + flagKey
            + " variation assignment for subject "
            + expectedSubjectAssignment.getSubjectKey();

    assertEquals(
        expectedSubjectAssignment.getAssignment().getVariation(),
        assignment.getVariation(),
        failureMessage);

    failureMessage =
        "Incorrect "
            + flagKey
            + " action assignment for subject "
            + expectedSubjectAssignment.getSubjectKey();

    assertEquals(
        expectedSubjectAssignment.getAssignment().getAction(),
        assignment.getAction(),
        failureMessage);
  }

  @Test
  public void testBanditLogsAction() {
    String flagKey = "banner_bandit_flag";
    String subjectKey = "bob";
    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("age", 25);
    subjectAttributes.put("country", "USA");
    subjectAttributes.put("gender_identity", "female");

    BanditActions actions = new BanditActions();

    Attributes nikeAttributes = new Attributes();
    nikeAttributes.put("brand_affinity", 1.5);
    nikeAttributes.put("loyalty_tier", "silver");
    actions.put("nike", nikeAttributes);

    Attributes adidasAttributes = new Attributes();
    adidasAttributes.put("brand_affinity", -1.0);
    adidasAttributes.put("loyalty_tier", "bronze");
    actions.put("adidas", adidasAttributes);

    Attributes rebookAttributes = new Attributes();
    rebookAttributes.put("brand_affinity", 0.5);
    rebookAttributes.put("loyalty_tier", "gold");
    actions.put("reebok", rebookAttributes);

    BanditResult banditResult =
        EppoClient.getInstance()
            .getBanditAction(flagKey, subjectKey, subjectAttributes, actions, "control");

    // Verify assignment
    assertEquals("banner_bandit", banditResult.getVariation());
    assertEquals("adidas", banditResult.getAction());

    Date inTheNearFuture = new Date(System.currentTimeMillis() + 1);

    // Verify experiment assignment log
    ArgumentCaptor<Assignment> assignmentLogCaptor = ArgumentCaptor.forClass(Assignment.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());
    Assignment capturedAssignment = assignmentLogCaptor.getValue();
    assertTrue(capturedAssignment.getTimestamp().after(testStart));
    assertTrue(capturedAssignment.getTimestamp().before(inTheNearFuture));
    assertEquals("banner_bandit_flag-training", capturedAssignment.getExperiment());
    assertEquals(flagKey, capturedAssignment.getFeatureFlag());
    assertEquals("training", capturedAssignment.getAllocation());
    assertEquals("banner_bandit", capturedAssignment.getVariation());
    assertEquals(subjectKey, capturedAssignment.getSubject());
    assertEquals(subjectAttributes, capturedAssignment.getSubjectAttributes());
    assertEquals("false", capturedAssignment.getMetaData().get("obfuscated"));

    // Verify bandit log
    ArgumentCaptor<BanditAssignment> banditLogCaptor =
        ArgumentCaptor.forClass(BanditAssignment.class);
    verify(mockBanditLogger, times(1)).logBanditAssignment(banditLogCaptor.capture());
    BanditAssignment capturedBanditAssignment = banditLogCaptor.getValue();
    assertTrue(capturedBanditAssignment.getTimestamp().after(testStart));
    assertTrue(capturedBanditAssignment.getTimestamp().before(inTheNearFuture));
    assertEquals(flagKey, capturedBanditAssignment.getFeatureFlag());
    assertEquals("banner_bandit", capturedBanditAssignment.getBandit());
    assertEquals(subjectKey, capturedBanditAssignment.getSubject());
    assertEquals("adidas", capturedBanditAssignment.getAction());
    assertEquals(0.099, capturedBanditAssignment.getActionProbability(), 0.0002);
    assertEquals("v123", capturedBanditAssignment.getModelVersion());

    Attributes expectedSubjectNumericAttributes = new Attributes();
    expectedSubjectNumericAttributes.put("age", 25);
    assertEquals(
        expectedSubjectNumericAttributes, capturedBanditAssignment.getSubjectNumericAttributes());

    Attributes expectedSubjectCategoricalAttributes = new Attributes();
    expectedSubjectCategoricalAttributes.put("country", "USA");
    expectedSubjectCategoricalAttributes.put("gender_identity", "female");
    assertEquals(
        expectedSubjectCategoricalAttributes,
        capturedBanditAssignment.getSubjectCategoricalAttributes());

    Attributes expectedActionNumericAttributes = new Attributes();
    expectedActionNumericAttributes.put("brand_affinity", -1.0);
    assertEquals(
        expectedActionNumericAttributes, capturedBanditAssignment.getActionNumericAttributes());

    Attributes expectedActionCategoricalAttributes = new Attributes();
    expectedActionCategoricalAttributes.put("loyalty_tier", "bronze");
    assertEquals(
        expectedActionCategoricalAttributes,
        capturedBanditAssignment.getActionCategoricalAttributes());

    assertEquals("false", capturedBanditAssignment.getMetaData().get("obfuscated"));
  }

  @Test
  public void testNoBanditLogsWhenNotBandit() {
    String flagKey = "banner_bandit_flag";
    String subjectKey = "anthony";
    Attributes subjectAttributes = new Attributes();

    BanditActions actions = new BanditActions();
    actions.put("nike", new Attributes());
    actions.put("adidas", new Attributes());

    BanditResult banditResult =
        EppoClient.getInstance()
            .getBanditAction(flagKey, subjectKey, subjectAttributes, actions, "default");

    // Verify assignment
    assertEquals("control", banditResult.getVariation());
    assertNull(banditResult.getAction());

    // Assignment won't log because the "analysis" allocation has doLog set to false
    ArgumentCaptor<Assignment> assignmentLogCaptor = ArgumentCaptor.forClass(Assignment.class);
    verify(mockAssignmentLogger, times(0)).logAssignment(assignmentLogCaptor.capture());
    // Bandit won't log because no bandit action was taken
    ArgumentCaptor<BanditAssignment> banditLogCaptor =
        ArgumentCaptor.forClass(BanditAssignment.class);
    verify(mockBanditLogger, times(0)).logBanditAssignment(banditLogCaptor.capture());
  }

  @Test
  public void testNoBanditLogsWhenNoActions() {
    String flagKey = "banner_bandit_flag";
    String subjectKey = "bob";
    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("age", 25);
    subjectAttributes.put("country", "USA");
    subjectAttributes.put("gender_identity", "female");

    BanditActions actions = new BanditActions();

    BanditResult banditResult =
        EppoClient.getInstance()
            .getBanditAction(flagKey, subjectKey, subjectAttributes, actions, "control");

    // Verify assignment
    assertEquals("banner_bandit", banditResult.getVariation());
    assertNull(banditResult.getAction());

    // The variation assignment should have been logged
    ArgumentCaptor<Assignment> assignmentLogCaptor = ArgumentCaptor.forClass(Assignment.class);
    verify(mockAssignmentLogger, times(1)).logAssignment(assignmentLogCaptor.capture());

    // No bandit log since no actions to consider
    ArgumentCaptor<BanditAssignment> banditLogCaptor =
        ArgumentCaptor.forClass(BanditAssignment.class);
    verify(mockBanditLogger, times(0)).logBanditAssignment(banditLogCaptor.capture());
  }

  @Test
  public void testBanditErrorGracefulModeOff() {
    EppoClient.getInstance()
        .setIsGracefulFailureMode(
            false); // Should be set by @BeforeEach but repeated here for test clarity
    try (MockedStatic<BanditEvaluator> mockedStatic = mockStatic(BanditEvaluator.class)) {
      // Configure the mock to throw an exception
      mockedStatic
          .when(() -> BanditEvaluator.evaluateBandit(anyString(), anyString(), any(), any(), any()))
          .thenThrow(new RuntimeException("Intentional Bandit Error"));

      // Assert that the exception is thrown when the method is called
      BanditActions actions = new BanditActions();
      actions.put("nike", new Attributes());
      actions.put("adidas", new Attributes());
      assertThrows(
          RuntimeException.class,
          () ->
              EppoClient.getInstance()
                  .getBanditAction(
                      "banner_bandit_flag", "subject", new Attributes(), actions, "default"));
    }
  }

  @Test
  public void testBanditErrorGracefulModeOn() {
    EppoClient.getInstance().setIsGracefulFailureMode(true);
    try (MockedStatic<BanditEvaluator> mockedStatic = mockStatic(BanditEvaluator.class)) {
      // Configure the mock to throw an exception
      mockedStatic
          .when(() -> BanditEvaluator.evaluateBandit(anyString(), anyString(), any(), any(), any()))
          .thenThrow(new RuntimeException("Intentional Bandit Error"));

      // Assert that the exception is thrown when the method is called
      BanditActions actions = new BanditActions();
      actions.put("nike", new Attributes());
      actions.put("adidas", new Attributes());
      BanditResult banditResult =
          EppoClient.getInstance()
              .getBanditAction(
                  "banner_bandit_flag", "subject", new Attributes(), actions, "default");
      assertEquals("banner_bandit", banditResult.getVariation());
      assertNull(banditResult.getAction());
    }
  }

  private static SimpleModule module() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(BanditTestCase.class, new BanditTestCaseDeserializer());
    return module;
  }
}

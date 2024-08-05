package cloud.eppo;

import cloud.eppo.helpers.*;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditAssignment;
import cloud.eppo.logging.BanditLogger;
import cloud.eppo.ufc.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class EppoClientBanditTest {
  private static final Logger log = LoggerFactory.getLogger(EppoClientBanditTest.class);
  private static final String DUMMY_BANDIT_API_KEY = "dummy-bandits-api-key"; // Will load bandit-flags-v1
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
  public void restMocks() {
    clearInvocations(mockAssignmentLogger);
    clearInvocations(mockBanditLogger);
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

    for (SubjectBanditAssignment subjectAssignment : testCase.getSubjects()) {
      String subjectKey = subjectAssignment.getSubjectKey();
      ContextAttributes attributes = subjectAssignment.getSubjectAttributes();
      Actions actions = subjectAssignment.getActions();
      BanditResult assignment = EppoClient.getInstance().getBanditAction(flagKey, subjectKey, attributes, actions, defaultValue);
      assertBanditAssignment(flagKey, subjectAssignment, assignment);
    }
  }

  /** Helper method for asserting a subject assignment with a useful failure message. */
  private void assertBanditAssignment(String flagKey, SubjectBanditAssignment expectedSubjectAssignment, BanditResult assignment) {
    String failureMessage =
      "Incorrect "
        + flagKey
        + " variation assignment for subject "
        + expectedSubjectAssignment.getSubjectKey();

    assertEquals(expectedSubjectAssignment.getAssignment().getVariation(), assignment.getVariation(), failureMessage);

    failureMessage =
      "Incorrect "
        + flagKey
        + " action assignment for subject "
        + expectedSubjectAssignment.getSubjectKey();

    assertEquals(expectedSubjectAssignment.getAssignment().getAction(), assignment.getAction(), failureMessage);
  }

  @Test
  public void testBanditLogging() {
    String flagKey = "banner_bandit_flag";
    String subjectKey = "bot";
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

    BanditResult banditResult = EppoClient.getInstance().getBanditAction(flagKey, subjectKey, subjectAttributes, actions, "control");


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
    ArgumentCaptor<BanditAssignment> banditLogCaptor = ArgumentCaptor.forClass(BanditAssignment.class);
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
    assertEquals(expectedSubjectNumericAttributes, capturedBanditAssignment.getSubjectNumericAttributes());

    Attributes expectedSubjectCategoricalAttributes = new Attributes();
    expectedSubjectCategoricalAttributes.put("country", "USA");
    expectedSubjectCategoricalAttributes.put("gender_identity", "female");
    assertEquals(expectedSubjectCategoricalAttributes, capturedBanditAssignment.getSubjectCategoricalAttributes());

    Attributes expectedActionNumericAttributes = new Attributes();
    expectedActionNumericAttributes.put("brand_affinity", -1.0);
    assertEquals(expectedActionNumericAttributes, capturedBanditAssignment.getActionNumericAttributes());

    Attributes expectedActionCategoricalAttributes = new Attributes();
    expectedActionCategoricalAttributes.put("loyalty_tier", "bronze");
    assertEquals(expectedActionCategoricalAttributes, capturedBanditAssignment.getActionCategoricalAttributes());

    assertEquals("false", capturedBanditAssignment.getMetaData().get("obfuscated"));
  }

  private static SimpleModule module() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(BanditTestCase.class, new BanditTestCaseDeserializer());
    return module;
  }
}

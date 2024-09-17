package cloud.eppo;

import static cloud.eppo.helpers.BanditTestCase.parseBanditTestCaseFile;
import static cloud.eppo.helpers.BanditTestCase.runBanditTestCase;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cloud.eppo.api.Attributes;
import cloud.eppo.api.BanditActions;
import cloud.eppo.api.BanditResult;
import cloud.eppo.helpers.*;
import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditAssignment;
import cloud.eppo.logging.BanditLogger;
import java.io.File;
import java.util.*;
import java.util.stream.Stream;
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

public class BaseEppoClientBanditTest {
  private static final Logger log = LoggerFactory.getLogger(BaseEppoClientBanditTest.class);
  private static final String DUMMY_BANDIT_API_KEY =
      "dummy-bandits-api-key"; // Will load bandit-flags-v1
  private static final String TEST_HOST =
      "https://us-central1-eppo-qa.cloudfunctions.net/serveGitHubRacTestFile";

  private static final AssignmentLogger mockAssignmentLogger = mock(AssignmentLogger.class);
  private static final BanditLogger mockBanditLogger = mock(BanditLogger.class);
  private static final Date testStart = new Date();

  private static BaseEppoClient eppoClient;

  // TODO: possibly consolidate code between this and the non-bandit test

  @BeforeAll
  public static void initClient() {
    eppoClient =
        new BaseEppoClient(
            DUMMY_BANDIT_API_KEY,
            "java",
            "3.0.0",
            TEST_HOST,
            mockAssignmentLogger,
            mockBanditLogger,
            false,
            false,
            null,
            null);

    eppoClient.loadConfiguration();

    log.info("Test client initialized");
  }

  private void initClientWithData(
      final String initialFlagConfiguration, final String initialBanditParameters) {

    eppoClient =
        new BaseEppoClient(
            DUMMY_BANDIT_API_KEY,
            "java",
            "3.0.0",
            TEST_HOST,
            mockAssignmentLogger,
            mockBanditLogger,
            false,
            false,
            initialFlagConfiguration,
            initialBanditParameters);
  }

  @BeforeEach
  public void reset() {
    clearInvocations(mockAssignmentLogger);
    clearInvocations(mockBanditLogger);
    doNothing().when(mockBanditLogger).logBanditAssignment(any());
    eppoClient.setIsGracefulFailureMode(false);
  }

  @ParameterizedTest
  @MethodSource("getBanditTestData")
  public void testUnobfuscatedBanditAssignments(File testFile) {
    BanditTestCase testCase = parseBanditTestCaseFile(testFile);
    runBanditTestCase(testCase, eppoClient);
  }

  public static Stream<Arguments> getBanditTestData() {
    return BanditTestCase.getBanditTestData();
  }

  @SuppressWarnings("ExtractMethodRecommender")
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
        eppoClient.getBanditAction(flagKey, subjectKey, subjectAttributes, actions, "control");

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
    assertEquals(7.1, capturedBanditAssignment.getOptimalityGap(), 0.0002);
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
        eppoClient.getBanditAction(flagKey, subjectKey, subjectAttributes, actions, "default");

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
        eppoClient.getBanditAction(flagKey, subjectKey, subjectAttributes, actions, "control");

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
    eppoClient.setIsGracefulFailureMode(
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
              eppoClient.getBanditAction(
                  "banner_bandit_flag", "subject", new Attributes(), actions, "default"));
    }
  }

  @Test
  public void testBanditErrorGracefulModeOn() {
    eppoClient.setIsGracefulFailureMode(true);
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
          eppoClient.getBanditAction(
              "banner_bandit_flag", "subject", new Attributes(), actions, "default");
      assertEquals("banner_bandit", banditResult.getVariation());
      assertNull(banditResult.getAction());
    }
  }

  @Test
  public void testBanditLogErrorNonFatal() {
    initClient();
    doThrow(new RuntimeException("Mock Bandit Logging Error"))
        .when(mockBanditLogger)
        .logBanditAssignment(any());

    BanditActions actions = new BanditActions();
    actions.put("nike", new Attributes());
    actions.put("adidas", new Attributes());
    BanditResult banditResult =
        eppoClient.getBanditAction(
            "banner_bandit_flag", "subject", new Attributes(), actions, "default");
    assertEquals("banner_bandit", banditResult.getVariation());
    assertEquals("nike", banditResult.getAction());

    ArgumentCaptor<BanditAssignment> banditLogCaptor =
        ArgumentCaptor.forClass(BanditAssignment.class);
    verify(mockBanditLogger, times(1)).logBanditAssignment(banditLogCaptor.capture());
  }

  @Test
  public void testWithInitialConfiguration() {
    String flagConfig =
        "{\"flags\": {\n"
            + "  \"banner_bandit_flag\": {\n"
            + "    \"key\": \"banner_bandit_flag\",\n"
            + "    \"enabled\": true,\n"
            + "    \"variationType\": \"STRING\",\n"
            + "    \"variations\": {\n"
            + "      \"banner_bandit\": {\n"
            + "        \"key\": \"banner_bandit\",\n"
            + "        \"value\": \"banner_bandit\"\n"
            + "      }\n"
            + "    },\n"
            + "    \"allocations\": [\n"
            + "      {\n"
            + "        \"key\": \"all\",\n"
            + "        \"rules\": [],\n"
            + "        \"splits\": [\n"
            + "          {\n"
            + "            \"variationKey\": \"banner_bandit\",\n"
            + "            \"shards\": []\n"
            + "          }\n"
            + "        ],\n"
            + "        \"doLog\": true\n"
            + "      }\n"
            + "    ],\n"
            + "    \"totalShards\": 10000\n"
            + "  }\n"
            + "},\n"
            + "\"banditReferences\": {\n"
            + "    \"banner_bandit\": {\n"
            + "      \"flagVariations\": [\n"
            + "        {\n"
            + "          \"key\": \"banner_bandit\",\n"
            + "          \"flagKey\": \"banner_bandit_flag\",\n"
            + "          \"allocationKey\": \"analysis\",\n"
            + "          \"variationKey\": \"banner_bandit\",\n"
            + "          \"variationValue\": \"banner_bandit\"\n"
            + "        }\n"
            + "      ],\n"
            + "      \"modelVersion\": \"v123\"\n"
            + "    }\n"
            + "  }\n"
            + "}";

    String banditConfig =
        "{\n"
            + "  \"bandits\": {\n"
            + "    \"banner_bandit\": {\n"
            + "      \"banditKey\": \"banner_bandit\",\n"
            + "      \"modelName\": \"falcon\",\n"
            + "      \"updatedAt\": \"2023-09-13T04:52:06.462Z\",\n"
            + "      \"modelVersion\": \"v123\",\n"
            + "      \"modelData\": {\n"
            + "        \"gamma\": 1.0,\n"
            + "        \"defaultActionScore\": 0.0,\n"
            + "        \"actionProbabilityFloor\": 0.0,\n"
            + "        \"coefficients\": {\n"
            + "          \"adidas\": {\n"
            + "            \"actionKey\": \"adidas\",\n"
            + "            \"intercept\": 1000,\n"
            + // Very large intercept make adidas win.
            "            \"actionNumericCoefficients\": [],\n"
            + "            \"actionCategoricalCoefficients\": [],\n"
            + "            \"subjectNumericCoefficients\": [],\n"
            + "            \"subjectCategoricalCoefficients\": []\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    initClientWithData(flagConfig, banditConfig);

    BanditActions actions = new BanditActions();
    actions.put("nike", new Attributes());
    actions.put("adidas", new Attributes());

    BanditResult result =
        eppoClient.getBanditAction(
            "banner_bandit_flag", "subject", new Attributes(), actions, "default");

    assertEquals("banner_bandit", result.getVariation());
    assertEquals("adidas", result.getAction());

    //     Demonstrate that loaded configuration is different from the initial string passed above.
    eppoClient.loadConfiguration();
    BanditResult banditResult =
        eppoClient.getBanditAction(
            "banner_bandit_flag", "subject", new Attributes(), actions, "default");
    assertEquals("banner_bandit", banditResult.getVariation());
    assertEquals("nike", banditResult.getAction());
  }
}

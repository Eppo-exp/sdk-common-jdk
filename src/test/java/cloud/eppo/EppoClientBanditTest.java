package cloud.eppo;

import cloud.eppo.helpers.*;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.ufc.dto.ContextAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
  private final ObjectMapper mapper = new ObjectMapper().registerModule(module());

  private AssignmentLogger mockAssignmentLogger;

  // TODO: possibly consolidate code between this and the non-bandit test

  private void initClient() {
    initClient(TEST_HOST, false, false, DUMMY_BANDIT_API_KEY);
  }

  private void initClient(
      String host, boolean isGracefulMode, boolean isConfigObfuscated, String apiKey) {
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

  @ParameterizedTest
  @MethodSource("getBanditTestData")
  public void testUnobfuscatedAssignments(File testFile) {
    initClient();
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

    EppoClient eppoClient = EppoClient.getInstance();
    String flagKey = testCase.getFlag();
    String defaultValue = testCase.getDefaultValue();

    for (SubjectBanditAssignment subjectAssignment : testCase.getSubjects()) {
      String subjectKey = subjectAssignment.getSubjectKey();
      ContextAttributes attributes = subjectAssignment.getSubjectAttributes();
      // TODO: set via getBanditAction
      String variationAssignment = null;
      String actionAssignment = null;

      assertBanditAssignment(flagKey, subjectAssignment, variationAssignment, actionAssignment);
    }
  }

  /** Helper method for asserting a subject assignment with a useful failure message. */
  private void assertBanditAssignment(String flagKey, SubjectBanditAssignment expectedSubjectAssignment, String variationAssignment, String actionAssignment) {
    String failureMessage =
        "Incorrect "
            + flagKey
            + " variation assignment for subject "
            + expectedSubjectAssignment.getSubjectKey();

    assertEquals(failureMessage, expectedSubjectAssignment.getVariationAssignment(), variationAssignment);

    failureMessage =
      "Incorrect "
        + flagKey
        + " action assignment for subject "
        + expectedSubjectAssignment.getSubjectKey();

    assertEquals(failureMessage, expectedSubjectAssignment.getActionAssignment(), actionAssignment);
  }

  private static SimpleModule module() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(BanditTestCase.class, new BanditTestCaseDeserializer());
    return module;
  }
}

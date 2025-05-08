package cloud.eppo.helpers;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cloud.eppo.BaseEppoClient;
import cloud.eppo.api.Actions;
import cloud.eppo.api.BanditResult;
import cloud.eppo.api.ContextAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.params.provider.Arguments;

public class BanditTestCase {
  private final String flag;
  private final String defaultValue;
  private final List<BanditSubjectAssignment> subjects;
  private String fileName;

  public BanditTestCase(String flag, String defaultValue, List<BanditSubjectAssignment> subjects) {
    this.flag = flag;
    this.defaultValue = defaultValue;
    this.subjects = subjects;
  }

  public String getFlag() {
    return flag;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public List<BanditSubjectAssignment> getSubjects() {
    return subjects;
  }

  public static Stream<Arguments> getBanditTestData() {
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

  private static final Gson gson = buildGson();

  public static Gson buildGson() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(BanditTestCase.class, new BanditTestCaseDeserializer());
    return gsonBuilder.create();
  }

  public static BanditTestCase parseBanditTestCaseFile(File testCaseFile) {
    BanditTestCase testCase;
    try {
      String json = FileUtils.readFileToString(testCaseFile, "UTF8");
      testCase = gson.fromJson(json, BanditTestCase.class);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return testCase;
  }

  public static void runBanditTestCase(BanditTestCase testCase, BaseEppoClient eppoClient) {
    assertFalse(testCase.getSubjects().isEmpty());

    String flagKey = testCase.getFlag();
    String defaultValue = testCase.getDefaultValue();

    for (BanditSubjectAssignment subjectAssignment : testCase.getSubjects()) {
      String subjectKey = subjectAssignment.getSubjectKey();
      ContextAttributes attributes = subjectAssignment.getSubjectAttributes();
      Actions actions = subjectAssignment.getActions();
      BanditResult assignment =
          eppoClient.getBanditAction(flagKey, subjectKey, attributes, actions, defaultValue);
      assertBanditAssignment(flagKey, subjectAssignment, assignment);
    }
  }

  /** Helper method for asserting a bandit assignment with a useful failure message. */
  private static void assertBanditAssignment(
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
}

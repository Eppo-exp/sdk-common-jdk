package cloud.eppo.helpers;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.BaseEppoClient;
import cloud.eppo.ufc.dto.Attributes;
import cloud.eppo.ufc.dto.VariationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.params.provider.Arguments;

public class AssignmentTestCase {
  private final String flag;
  private final VariationType variationType;
  private final TestCaseValue defaultValue;
  private final List<SubjectAssignment> subjects;

  public AssignmentTestCase(
      String flag,
      VariationType variationType,
      TestCaseValue defaultValue,
      List<SubjectAssignment> subjects) {
    this.flag = flag;
    this.variationType = variationType;
    this.defaultValue = defaultValue;
    this.subjects = subjects;
  }

  public String getFlag() {
    return flag;
  }

  public VariationType getVariationType() {
    return variationType;
  }

  public TestCaseValue getDefaultValue() {
    return defaultValue;
  }

  public List<SubjectAssignment> getSubjects() {
    return subjects;
  }

  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(assignmentTestCaseModule());

  public static SimpleModule assignmentTestCaseModule() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(AssignmentTestCase.class, new AssignmentTestCaseDeserializer());
    return module;
  }

  public static Stream<Arguments> getAssignmentTestData() {
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

  public static AssignmentTestCase parseTestCaseFile(File testCaseFile) {
    AssignmentTestCase testCase;
    try {
      String json = FileUtils.readFileToString(testCaseFile, "UTF8");

      testCase = mapper.readValue(json, AssignmentTestCase.class);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return testCase;
  }

  public static void runTestCase(AssignmentTestCase testCase, BaseEppoClient eppoClient) {
    String flagKey = testCase.getFlag();
    TestCaseValue defaultValue = testCase.getDefaultValue();
    assertFalse(testCase.getSubjects().isEmpty());

    for (SubjectAssignment subjectAssignment : testCase.getSubjects()) {
      String subjectKey = subjectAssignment.getSubjectKey();
      Attributes subjectAttributes = subjectAssignment.getSubjectAttributes();

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
                  flagKey, subjectKey, subjectAttributes, testCase.getDefaultValue().jsonValue());
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
  private static <T> void assertAssignment(
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
          expectedSubjectAssignment.getAssignment().booleanValue(), assignment, failureMessage);
    } else if (assignment instanceof Integer) {
      assertEquals(
          Double.valueOf(expectedSubjectAssignment.getAssignment().doubleValue()).intValue(),
          assignment,
          failureMessage);
    } else if (assignment instanceof Double) {
      assertEquals(
          expectedSubjectAssignment.getAssignment().doubleValue(),
          (Double) assignment,
          0.000001,
          failureMessage);
    } else if (assignment instanceof String) {
      assertEquals(
          expectedSubjectAssignment.getAssignment().stringValue(), assignment, failureMessage);
    } else if (assignment instanceof JsonNode) {
      assertEquals(
          expectedSubjectAssignment.getAssignment().jsonValue().toString(),
          assignment.toString(),
          failureMessage);
    } else {
      throw new IllegalArgumentException(
          "Unexpected assignment type " + assignment.getClass().getCanonicalName());
    }
  }
}

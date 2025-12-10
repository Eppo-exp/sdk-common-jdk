package cloud.eppo.helpers;

import cloud.eppo.api.Attributes;
import cloud.eppo.api.EvaluationDetails;

public class SubjectAssignment {
  private final String subjectKey;
  private final Attributes subjectAttributes;
  private final TestCaseValue assignment;
  private final EvaluationDetails evaluationDetails; // Optional: for validating details

  public SubjectAssignment(
      String subjectKey, Attributes subjectAttributes, TestCaseValue assignment) {
    this(subjectKey, subjectAttributes, assignment, null);
  }

  public SubjectAssignment(
      String subjectKey,
      Attributes subjectAttributes,
      TestCaseValue assignment,
      EvaluationDetails evaluationDetails) {
    this.subjectKey = subjectKey;
    this.subjectAttributes = subjectAttributes;
    this.assignment = assignment;
    this.evaluationDetails = evaluationDetails;
  }

  public String getSubjectKey() {
    return subjectKey;
  }

  public Attributes getSubjectAttributes() {
    return subjectAttributes;
  }

  public TestCaseValue getAssignment() {
    return assignment;
  }

  public EvaluationDetails getEvaluationDetails() {
    return evaluationDetails;
  }

  public boolean hasEvaluationDetails() {
    return evaluationDetails != null;
  }
}

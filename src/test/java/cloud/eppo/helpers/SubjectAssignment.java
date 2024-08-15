package cloud.eppo.helpers;

import cloud.eppo.ufc.dto.Attributes;

public class SubjectAssignment {
  private final String subjectKey;
  private final Attributes subjectAttributes;
  private final TestCaseValue assignment;

  public SubjectAssignment(
      String subjectKey, Attributes subjectAttributes, TestCaseValue assignment) {
    this.subjectKey = subjectKey;
    this.subjectAttributes = subjectAttributes;
    this.assignment = assignment;
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
}

package cloud.eppo.helpers;

import cloud.eppo.ufc.dto.Actions;
import cloud.eppo.ufc.dto.BanditResult;
import cloud.eppo.ufc.dto.ContextAttributes;

public class SubjectBanditAssignment {
  private final String subjectKey;
  private final ContextAttributes subjectAttributes;
  private final Actions actions;
  private final BanditResult assignment;

  public SubjectBanditAssignment(
      String subjectKey, ContextAttributes attributes, Actions actions, BanditResult assignment) {
    this.subjectKey = subjectKey;
    this.subjectAttributes = attributes;
    this.actions = actions;
    this.assignment = assignment;
  }

  public String getSubjectKey() {
    return subjectKey;
  }

  public ContextAttributes getSubjectAttributes() {
    return subjectAttributes;
  }

  public Actions getActions() {
    return actions;
  }

  public BanditResult getAssignment() {
    return assignment;
  }
}

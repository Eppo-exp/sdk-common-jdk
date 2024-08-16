package cloud.eppo.helpers;

import cloud.eppo.api.Actions;
import cloud.eppo.api.BanditResult;
import cloud.eppo.api.ContextAttributes;

public class BanditSubjectAssignment {
  private final String subjectKey;
  private final ContextAttributes subjectAttributes;
  private final Actions actions;
  private final BanditResult assignment;

  public BanditSubjectAssignment(
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

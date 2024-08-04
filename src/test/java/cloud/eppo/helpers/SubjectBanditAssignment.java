package cloud.eppo.helpers;

import cloud.eppo.ufc.dto.BanditResult;
import cloud.eppo.ufc.dto.ContextAttributes;
import cloud.eppo.ufc.dto.DiscriminableAttributes;

import java.util.Map;

public class SubjectBanditAssignment {
  private final String subjectKey;
  private final ContextAttributes subjectAttributes;
  private final Map<String, DiscriminableAttributes> actions;
  private final BanditResult assignment;

  public SubjectBanditAssignment(
    String subjectKey, ContextAttributes attributes, Map<String, DiscriminableAttributes> actions, BanditResult assignment) {
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

  public Map<String, DiscriminableAttributes> getActions() {
    return actions;
  }

  public BanditResult getAssignment() {
    return assignment;
  }
}

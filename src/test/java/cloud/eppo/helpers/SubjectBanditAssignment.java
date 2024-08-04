package cloud.eppo.helpers;

import cloud.eppo.ufc.dto.ContextAttributes;
import cloud.eppo.ufc.dto.DiscriminableAttributes;

import java.util.Map;

public class SubjectBanditAssignment {
  private final String subjectKey;
  private final ContextAttributes subjectAttributes;
  private final Map<String, DiscriminableAttributes> actions;
  private final String variationAssignment;
  private final String actionAssignment;

  public SubjectBanditAssignment(
    String subjectKey, ContextAttributes attributes, Map<String, DiscriminableAttributes> actions, String variationAssignment, String actionAssignment) {
    this.subjectKey = subjectKey;
    this.subjectAttributes = attributes;
    this.actions = actions;
    this.variationAssignment = variationAssignment;
    this.actionAssignment = actionAssignment;
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

  public String getVariationAssignment() {
    return variationAssignment;
  }

  public String getActionAssignment() {
    return actionAssignment;
  }
}

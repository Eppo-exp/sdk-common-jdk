package cloud.eppo.ufc.dto;

import java.util.Set;

public class TargetingRule {
  private final Set<TargetingCondition> conditions;

  public TargetingRule(Set<TargetingCondition> conditions) {
    this.conditions = conditions;
  }

  public Set<TargetingCondition> getConditions() {
    return conditions;
  }
}

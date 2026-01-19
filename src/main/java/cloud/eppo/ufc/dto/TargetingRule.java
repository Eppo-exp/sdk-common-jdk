package cloud.eppo.ufc.dto;

import cloud.eppo.api.ITargetingRule;
import java.util.Objects;
import java.util.Set;

public class TargetingRule implements ITargetingRule {
  private final Set<TargetingCondition> conditions;

  public TargetingRule(Set<TargetingCondition> conditions) {
    this.conditions = conditions;
  }

  public Set<TargetingCondition> getConditions() {
    return conditions;
  }

  @Override
  public String toString() {
    return "TargetingRule{" + "conditions=" + conditions + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    TargetingRule that = (TargetingRule) o;
    return Objects.equals(conditions, that.conditions);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(conditions);
  }
}

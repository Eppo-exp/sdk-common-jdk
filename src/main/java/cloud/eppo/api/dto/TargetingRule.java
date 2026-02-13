package cloud.eppo.api.dto;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public interface TargetingRule extends Serializable {
  @NotNull Set<TargetingCondition> getConditions();

  class Default implements TargetingRule {
    private static final long serialVersionUID = 1L;
    private final Set<TargetingCondition> conditions;

    public Default(Set<TargetingCondition> conditions) {
      this.conditions = conditions;
    }

    @Override
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
      return Objects.equals(conditions, that.getConditions());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(conditions);
    }
  }
}

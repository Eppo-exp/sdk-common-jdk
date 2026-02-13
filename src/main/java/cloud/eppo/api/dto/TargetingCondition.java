package cloud.eppo.api.dto;

import cloud.eppo.api.EppoValue;
import java.io.Serializable;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public interface TargetingCondition extends Serializable {
  @NotNull OperatorType getOperator();

  @NotNull String getAttribute();

  @NotNull EppoValue getValue();

  class Default implements TargetingCondition {
    private static final long serialVersionUID = 1L;
    private final @NotNull OperatorType operator;
    private final @NotNull String attribute;
    private final @NotNull EppoValue value;

    public Default(
        @NotNull OperatorType operator,
        @NotNull String attribute,
        @NotNull EppoValue value) {
      this.operator = operator;
      this.attribute = attribute;
      this.value = value;
    }

    @Override
    public String toString() {
      return "TargetingCondition{"
          + "operator="
          + operator
          + ", attribute='"
          + attribute
          + '\''
          + ", value="
          + value
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      TargetingCondition that = (TargetingCondition) o;
      return operator == that.getOperator()
          && Objects.equals(attribute, that.getAttribute())
          && Objects.equals(value, that.getValue());
    }

    @Override
    public int hashCode() {
      return Objects.hash(operator, attribute, value);
    }

    @Override
    @NotNull
    public OperatorType getOperator() {
      return operator;
    }

    @Override
    @NotNull
    public String getAttribute() {
      return attribute;
    }

    @Override
    @NotNull
    public EppoValue getValue() {
      return value;
    }
  }
}

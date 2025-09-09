package cloud.eppo.ufc.dto;

import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import cloud.eppo.api.EppoValue;

public class TargetingCondition {
  @NotNull private final OperatorType operator;
  @NotNull private final String attribute;
  @NotNull private final EppoValue value;

  public TargetingCondition(
      @NotNull OperatorType operator,
      @NotNull String attribute,
      @NotNull EppoValue value) {
    throwIfNull(operator, "operator must not be null");
    throwIfNull(attribute, "attribute must not be null");
    throwIfNull(value, "value must not be null");

    this.operator = operator;
    this.attribute = attribute;
    this.value = value;
  }

  @Override @NotNull
  public String toString() {
    return "TargetingCondition{" +
      "operator=" + operator +
      ", attribute='" + attribute + '\'' +
      ", value=" + value +
      '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    TargetingCondition that = (TargetingCondition) o;
    return operator == that.operator
            && Objects.equals(attribute, that.attribute)
            && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operator, attribute, value);
  }

  @NotNull
  public OperatorType getOperator() {
    return operator;
  }

  @NotNull
  public String getAttribute() {
    return attribute;
  }

  @NotNull
  public EppoValue getValue() {
    return value;
  }
}

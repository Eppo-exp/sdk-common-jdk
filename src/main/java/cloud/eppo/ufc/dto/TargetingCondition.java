package cloud.eppo.ufc.dto;

import java.util.Objects;

import cloud.eppo.api.EppoValue;
import cloud.eppo.api.dto.OperatorType;

public class TargetingCondition {
  private final OperatorType operator;
  private final String attribute;
  private final EppoValue value;

  public TargetingCondition(OperatorType operator, String attribute, EppoValue value) {
    this.operator = operator;
    this.attribute = attribute;
    this.value = value;
  }

  @Override
  public String toString() {
    return "TargetingCondition{" +
      "operator=" + operator +
      ", attribute='" + attribute + '\'' +
      ", value=" + value +
      '}';
  }

  @Override
  public boolean equals(Object o) {
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

  public OperatorType getOperator() {
    return operator;
  }

  public String getAttribute() {
    return attribute;
  }

  public EppoValue getValue() {
    return value;
  }
}

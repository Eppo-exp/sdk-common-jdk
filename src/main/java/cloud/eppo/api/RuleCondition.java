package cloud.eppo.api;

import java.util.Objects;

/** Represents a single condition within a targeting rule. */
public class RuleCondition {
  private final String attribute;
  private final String operator;
  private final EppoValue value;

  public RuleCondition(String attribute, String operator, EppoValue value) {
    this.attribute = attribute;
    this.operator = operator;
    this.value = value;
  }

  public String getAttribute() {
    return attribute;
  }

  public String getOperator() {
    return operator;
  }

  public EppoValue getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RuleCondition that = (RuleCondition) o;
    return Objects.equals(attribute, that.attribute)
        && Objects.equals(operator, that.operator)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attribute, operator, value);
  }

  @Override
  public String toString() {
    return "RuleCondition{"
        + "attribute='"
        + attribute
        + '\''
        + ", operator='"
        + operator
        + '\''
        + ", value="
        + value
        + '}';
  }
}

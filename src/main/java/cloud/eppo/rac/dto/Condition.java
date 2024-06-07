package cloud.eppo.rac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Rule's Condition Class */
public class Condition {
  private final OperatorType operator;
  private final String attribute;
  private final EppoValue value;

  @JsonCreator
  public Condition(
      @JsonProperty("operator") OperatorType operator,
      @JsonProperty("attribute") String attribute,
      @JsonProperty("value") EppoValue value) {
    this.operator = operator;
    this.attribute = attribute;
    this.value = value;
  }

  @Override
  public String toString() {
    return "[Operator: "
        + operator
        + " | Attribute: "
        + attribute
        + " | Value: "
        + value.toString()
        + "]";
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

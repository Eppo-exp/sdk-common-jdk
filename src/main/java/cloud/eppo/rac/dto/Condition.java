package cloud.eppo.rac.dto;

/** Rule's Condition Class */
public class Condition {
  private final OperatorType operator;
  private final String attribute;
  private final EppoValue value;

  public Condition(OperatorType operator, String attribute, EppoValue value) {
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

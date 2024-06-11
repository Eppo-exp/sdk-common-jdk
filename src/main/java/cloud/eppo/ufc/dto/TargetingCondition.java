package cloud.eppo.ufc.dto;

public class TargetingCondition {
  private final OperatorType operator;
  private final String attribute;
  private final EppoValue value;

  public TargetingCondition(OperatorType operator, String attribute, EppoValue value) {
    this.operator = operator;
    this.attribute = attribute;
    this.value = value;
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

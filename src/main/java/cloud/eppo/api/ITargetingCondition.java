package cloud.eppo.api;

/** Interface for TargetingCondition allowing downstream SDKs to provide custom implementations. */
public interface ITargetingCondition {
  OperatorType getOperator();

  String getAttribute();

  IEppoValue getValue();
}

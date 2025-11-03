package cloud.eppo;

/**
 * Possible reasons for a default value from a call to BaseEppoClient.getTypedAssignmentResult()
 */
public enum ValuedFlagEvaluationResultType {
  NO_FLAG_CONFIG,
  FLAG_DISABLED,
  BAD_VARIATION_TYPE,
  BAD_VALUE_TYPE,
  NO_ALLOCATION,
  OK,
  ;
}

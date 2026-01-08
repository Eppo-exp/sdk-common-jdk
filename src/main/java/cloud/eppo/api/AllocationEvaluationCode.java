package cloud.eppo.api;

/**
 * Enum representing the result code of an allocation evaluation within a flag.
 *
 * <p>Allocations are evaluated in order, and this code indicates why an allocation was or was not
 * selected.
 */
public enum AllocationEvaluationCode {
  /** Allocation rules matched and the allocation was selected. */
  MATCH("MATCH"),

  /** Allocation rules did not match the subject attributes. */
  FAILING_RULE("FAILING_RULE"),

  /** Current time is before the allocation's start time. */
  BEFORE_START_TIME("BEFORE_START_TIME"),

  /** Current time is after the allocation's end time. */
  AFTER_END_TIME("AFTER_END_TIME"),

  /** Subject was not selected due to traffic exposure percentage. */
  TRAFFIC_EXPOSURE_MISS("TRAFFIC_EXPOSURE_MISS"),

  /** Allocation was not evaluated (e.g., a previous allocation matched). */
  UNEVALUATED("UNEVALUATED");

  private final String code;

  AllocationEvaluationCode(String code) {
    this.code = code;
  }

  /** Returns the string representation of this allocation evaluation code. */
  public String getCode() {
    return code;
  }

  /**
   * Parses a string code into an AllocationEvaluationCode enum.
   *
   * @param code the string code to parse
   * @return the corresponding AllocationEvaluationCode, or null if not recognized
   */
  public static AllocationEvaluationCode fromString(String code) {
    if (code == null) {
      return null;
    }
    for (AllocationEvaluationCode evaluationCode : values()) {
      if (evaluationCode.code.equals(code)) {
        return evaluationCode;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return code;
  }
}

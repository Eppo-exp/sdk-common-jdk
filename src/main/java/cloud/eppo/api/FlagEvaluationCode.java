package cloud.eppo.api;

/**
 * Enum representing the result code of a flag evaluation.
 *
 * <p>Use {@link #isError()} to determine if the evaluation resulted in an error state.
 */
public enum FlagEvaluationCode {
  /** Flag was successfully evaluated and a variation was assigned. */
  MATCH("MATCH", false),

  /** Flag was not found or is disabled. */
  FLAG_UNRECOGNIZED_OR_DISABLED("FLAG_UNRECOGNIZED_OR_DISABLED", true),

  /** The flag's type doesn't match the requested type. */
  TYPE_MISMATCH("TYPE_MISMATCH", true),

  /** The variation value is incompatible with the flag's declared type. */
  ASSIGNMENT_ERROR("ASSIGNMENT_ERROR", true),

  /** No allocations were configured for the flag. */
  DEFAULT_ALLOCATION_NULL("DEFAULT_ALLOCATION_NULL", true),

  /** Flag evaluation succeeded but bandit evaluation failed. */
  BANDIT_ERROR("BANDIT_ERROR", true);

  private final String code;
  private final boolean isError;

  FlagEvaluationCode(String code, boolean isError) {
    this.code = code;
    this.isError = isError;
  }

  /** Returns the string representation of this evaluation code. */
  public String getCode() {
    return code;
  }

  /**
   * Returns true if this evaluation code represents an error state.
   *
   * @return true if the evaluation failed and the default value should be used
   */
  public boolean isError() {
    return isError;
  }

  /**
   * Parses a string code into a FlagEvaluationCode enum.
   *
   * @param code the string code to parse
   * @return the corresponding FlagEvaluationCode, or null if not recognized
   */
  public static FlagEvaluationCode fromString(String code) {
    if (code == null) {
      return null;
    }
    for (FlagEvaluationCode evaluationCode : values()) {
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

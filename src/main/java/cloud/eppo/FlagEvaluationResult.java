package cloud.eppo;

import cloud.eppo.api.Attributes;
import cloud.eppo.ufc.dto.Variation;
import java.util.Map;
import java.util.Objects;

public class FlagEvaluationResult {

  private final String flagKey;
  private final String subjectKey;
  private final Attributes subjectAttributes;
  private final String allocationKey;
  private final Variation variation;
  private final Map<String, String> extraLogging;
  private final boolean doLog;

  public FlagEvaluationResult(
      String flagKey,
      String subjectKey,
      Attributes subjectAttributes,
      String allocationKey,
      Variation variation,
      Map<String, String> extraLogging,
      boolean doLog) {
    this.flagKey = flagKey;
    this.subjectKey = subjectKey;
    this.subjectAttributes = subjectAttributes;
    this.allocationKey = allocationKey;
    this.variation = variation;
    this.extraLogging = extraLogging;
    this.doLog = doLog;
  }

  @Override
  public String toString() {
    return "FlagEvaluationResult{" +
      "flagKey='" + flagKey + '\'' +
      ", subjectKey='" + subjectKey + '\'' +
      ", subjectAttributes=" + subjectAttributes +
      ", allocationKey='" + allocationKey + '\'' +
      ", variation=" + variation +
      ", extraLogging=" + extraLogging +
      ", doLog=" + doLog +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    FlagEvaluationResult that = (FlagEvaluationResult) o;
    return doLog == that.doLog
            && Objects.equals(flagKey, that.flagKey)
            && Objects.equals(subjectKey, that.subjectKey)
            && Objects.equals(subjectAttributes, that.subjectAttributes)
            && Objects.equals(allocationKey, that.allocationKey)
            && Objects.equals(variation, that.variation)
            && Objects.equals(extraLogging, that.extraLogging);
  }

  @Override
  public int hashCode() {
    return Objects.hash(flagKey, subjectKey, subjectAttributes, allocationKey, variation, extraLogging, doLog);
  }

  public String getFlagKey() {
    return flagKey;
  }

  public String getSubjectKey() {
    return subjectKey;
  }

  public Attributes getSubjectAttributes() {
    return subjectAttributes;
  }

  public String getAllocationKey() {
    return allocationKey;
  }

  public Variation getVariation() {
    return variation;
  }

  public Map<String, String> getExtraLogging() {
    return extraLogging;
  }

  public boolean doLog() {
    return doLog;
  }
}

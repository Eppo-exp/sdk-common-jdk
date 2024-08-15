package cloud.eppo;

import cloud.eppo.ufc.dto.Attributes;
import cloud.eppo.ufc.dto.Variation;
import java.util.Map;

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

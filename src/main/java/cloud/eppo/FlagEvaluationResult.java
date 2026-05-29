package cloud.eppo;

import cloud.eppo.api.*;
import cloud.eppo.api.dto.Variation;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/** Flag evaluation result that includes detailed evaluation information. */
public class FlagEvaluationResult {

  private final String flagKey;
  private final String subjectKey;
  private final Attributes subjectAttributes;
  private final String allocationKey;
  private final Variation variation;
  private final Map<String, String> extraLogging;
  private final boolean doLog;
  private final EvaluationDetails evaluationDetails;

  public FlagEvaluationResult(
      String flagKey,
      String subjectKey,
      Attributes subjectAttributes,
      String allocationKey,
      Variation variation,
      Map<String, String> extraLogging,
      boolean doLog,
      EvaluationDetails evaluationDetails) {
    this.flagKey = flagKey;
    this.subjectKey = subjectKey;
    this.subjectAttributes = subjectAttributes;
    this.allocationKey = allocationKey;
    this.variation = variation;
    this.extraLogging = extraLogging;
    this.doLog = doLog;
    this.evaluationDetails = evaluationDetails;
  }

  @Override
  public String toString() {
    return "FlagEvaluationResult{"
        + "flagKey='"
        + flagKey
        + '\''
        + ", subjectKey='"
        + subjectKey
        + '\''
        + ", subjectAttributes="
        + subjectAttributes
        + ", allocationKey='"
        + allocationKey
        + '\''
        + ", variation="
        + variation
        + ", extraLogging="
        + extraLogging
        + ", doLog="
        + doLog
        + ", evaluationDetails="
        + evaluationDetails
        + '}';
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
        && Objects.equals(extraLogging, that.extraLogging)
        && Objects.equals(evaluationDetails, that.evaluationDetails);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        flagKey,
        subjectKey,
        subjectAttributes,
        allocationKey,
        variation,
        extraLogging,
        doLog,
        evaluationDetails);
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

  public EvaluationDetails getEvaluationDetails() {
    return evaluationDetails;
  }

  /** Builder to construct flag evaluation results during flag evaluation. */
  public static class Builder {
    private String flagKey;
    private String subjectKey;
    private Attributes subjectAttributes;
    private String allocationKey;
    private Variation variation;
    private Map<String, String> extraLogging;
    private boolean doLog;

    // Delegate to EvaluationDetails.Builder for evaluation details
    private final EvaluationDetails.Builder detailsBuilder = EvaluationDetails.builder();

    public Builder flagKey(String flagKey) {
      this.flagKey = flagKey;
      return this;
    }

    public Builder subjectKey(String subjectKey) {
      this.subjectKey = subjectKey;
      return this;
    }

    public Builder subjectAttributes(Attributes subjectAttributes) {
      this.subjectAttributes = subjectAttributes;
      return this;
    }

    public Builder allocationKey(String allocationKey) {
      this.allocationKey = allocationKey;
      return this;
    }

    public Builder variation(Variation variation) {
      this.variation = variation;
      return this;
    }

    public Builder extraLogging(Map<String, String> extraLogging) {
      this.extraLogging = extraLogging;
      return this;
    }

    public Builder doLog(boolean doLog) {
      this.doLog = doLog;
      return this;
    }

    public Builder environmentName(String environmentName) {
      detailsBuilder.environmentName(environmentName);
      return this;
    }

    public Builder flagEvaluationCode(FlagEvaluationCode code) {
      detailsBuilder.flagEvaluationCode(code);
      return this;
    }

    public Builder flagEvaluationDescription(String description) {
      detailsBuilder.flagEvaluationDescription(description);
      return this;
    }

    public Builder banditKey(String banditKey) {
      detailsBuilder.banditKey(banditKey);
      return this;
    }

    public Builder banditAction(String banditAction) {
      detailsBuilder.banditAction(banditAction);
      return this;
    }

    public Builder matchedRule(MatchedRule matchedRule) {
      detailsBuilder.matchedRule(matchedRule);
      return this;
    }

    public Builder matchedAllocation(AllocationDetails matchedAllocation) {
      detailsBuilder.matchedAllocation(matchedAllocation);
      return this;
    }

    public Builder addUnmatchedAllocation(AllocationDetails allocation) {
      detailsBuilder.addUnmatchedAllocation(allocation);
      return this;
    }

    public Builder addUnevaluatedAllocation(AllocationDetails allocation) {
      detailsBuilder.addUnevaluatedAllocation(allocation);
      return this;
    }

    public Builder configFetchedAt(Date configFetchedAt) {
      detailsBuilder.configFetchedAt(configFetchedAt);
      return this;
    }

    public Builder configPublishedAt(Date configPublishedAt) {
      detailsBuilder.configPublishedAt(configPublishedAt);
      return this;
    }

    public FlagEvaluationResult build() {
      // Set variation details before building
      if (variation != null) {
        detailsBuilder.variationKey(variation.getKey());
        detailsBuilder.variationValue(variation.getValue());
      }

      return new FlagEvaluationResult(
          flagKey,
          subjectKey,
          subjectAttributes,
          allocationKey,
          variation,
          extraLogging,
          doLog,
          detailsBuilder.build());
    }
  }
}

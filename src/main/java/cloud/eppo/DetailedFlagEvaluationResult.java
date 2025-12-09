package cloud.eppo;

import cloud.eppo.api.*;
import cloud.eppo.ufc.dto.Variation;
import java.util.Date;
import java.util.Map;

/**
 * Extended flag evaluation result that includes detailed evaluation information for debugging and
 * understanding flag assignments.
 */
public class DetailedFlagEvaluationResult extends FlagEvaluationResult {
  private final EvaluationDetails evaluationDetails;

  public DetailedFlagEvaluationResult(
      String flagKey,
      String subjectKey,
      Attributes subjectAttributes,
      String allocationKey,
      Variation variation,
      Map<String, String> extraLogging,
      boolean doLog,
      EvaluationDetails evaluationDetails) {
    super(flagKey, subjectKey, subjectAttributes, allocationKey, variation, extraLogging, doLog);
    this.evaluationDetails = evaluationDetails;
  }

  public EvaluationDetails getEvaluationDetails() {
    return evaluationDetails;
  }

  /** Builder to construct detailed evaluation results during flag evaluation. */
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

    public DetailedFlagEvaluationResult build() {
      // Set variation details before building
      if (variation != null) {
        detailsBuilder.variationKey(variation.getKey());
        detailsBuilder.variationValue(variation.getValue());
      }

      return new DetailedFlagEvaluationResult(
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

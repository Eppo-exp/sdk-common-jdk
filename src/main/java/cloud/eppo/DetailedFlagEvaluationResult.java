package cloud.eppo;

import cloud.eppo.api.*;
import cloud.eppo.ufc.dto.Variation;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    // Evaluation details fields
    private String environmentName = "Test"; // Default for now
    private FlagEvaluationCode flagEvaluationCode;
    private String flagEvaluationDescription;
    private String banditKey;
    private String banditAction;
    private MatchedRule matchedRule;
    private AllocationDetails matchedAllocation;
    private final List<AllocationDetails> unmatchedAllocations = new ArrayList<>();
    private final List<AllocationDetails> unevaluatedAllocations = new ArrayList<>();
    private Date configFetchedAt;
    private Date configPublishedAt;

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
      this.environmentName = environmentName;
      return this;
    }

    public Builder flagEvaluationCode(FlagEvaluationCode code) {
      this.flagEvaluationCode = code;
      return this;
    }

    public Builder flagEvaluationDescription(String description) {
      this.flagEvaluationDescription = description;
      return this;
    }

    public Builder banditKey(String banditKey) {
      this.banditKey = banditKey;
      return this;
    }

    public Builder banditAction(String banditAction) {
      this.banditAction = banditAction;
      return this;
    }

    public Builder matchedRule(MatchedRule matchedRule) {
      this.matchedRule = matchedRule;
      return this;
    }

    public Builder matchedAllocation(AllocationDetails matchedAllocation) {
      this.matchedAllocation = matchedAllocation;
      return this;
    }

    public Builder addUnmatchedAllocation(AllocationDetails allocation) {
      this.unmatchedAllocations.add(allocation);
      return this;
    }

    public Builder addUnevaluatedAllocation(AllocationDetails allocation) {
      this.unevaluatedAllocations.add(allocation);
      return this;
    }

    public Builder configFetchedAt(Date configFetchedAt) {
      this.configFetchedAt = configFetchedAt;
      return this;
    }

    public Builder configPublishedAt(Date configPublishedAt) {
      this.configPublishedAt = configPublishedAt;
      return this;
    }

    public DetailedFlagEvaluationResult build() {
      // Build evaluation details
      String variationKey = variation != null ? variation.getKey() : null;
      EppoValue variationValue = variation != null ? variation.getValue() : null;

      EvaluationDetails details =
          new EvaluationDetails(
              environmentName,
              configFetchedAt,
              configPublishedAt,
              flagEvaluationCode,
              flagEvaluationDescription,
              banditKey,
              banditAction,
              variationKey,
              variationValue,
              matchedRule,
              matchedAllocation,
              new ArrayList<>(unmatchedAllocations),
              new ArrayList<>(unevaluatedAllocations));

      return new DetailedFlagEvaluationResult(
          flagKey,
          subjectKey,
          subjectAttributes,
          allocationKey,
          variation,
          extraLogging,
          doLog,
          details);
    }

    private static Object getEppoValueAsObject(EppoValue value) {
      if (value.isNull()) {
        return null;
      } else if (value.isBoolean()) {
        return value.booleanValue();
      } else if (value.isNumeric()) {
        return value.doubleValue();
      } else if (value.isString()) {
        return value.stringValue();
      } else if (value.isStringArray()) {
        return value.stringArrayValue();
      }
      return null;
    }
  }
}

package cloud.eppo.api;

import java.util.List;

/**
 * Contains comprehensive debugging information about a flag evaluation. This includes why a
 * particular variation was assigned, which allocations matched or didn't match, and other metadata
 * useful for understanding flag behavior.
 */
public class EvaluationDetails {
  private final String environmentName;
  private final FlagEvaluationCode flagEvaluationCode;
  private final String flagEvaluationDescription;
  private final String banditKey;
  private final String banditAction;
  private final String variationKey;
  private final EppoValue variationValue;
  private final MatchedRule matchedRule;
  private final AllocationDetails matchedAllocation;
  private final List<AllocationDetails> unmatchedAllocations;
  private final List<AllocationDetails> unevaluatedAllocations;

  public EvaluationDetails(
      String environmentName,
      FlagEvaluationCode flagEvaluationCode,
      String flagEvaluationDescription,
      String banditKey,
      String banditAction,
      String variationKey,
      EppoValue variationValue,
      MatchedRule matchedRule,
      AllocationDetails matchedAllocation,
      List<AllocationDetails> unmatchedAllocations,
      List<AllocationDetails> unevaluatedAllocations) {
    this.environmentName = environmentName;
    this.flagEvaluationCode = flagEvaluationCode;
    this.flagEvaluationDescription = flagEvaluationDescription;
    this.banditKey = banditKey;
    this.banditAction = banditAction;
    this.variationKey = variationKey;
    this.variationValue = variationValue;
    this.matchedRule = matchedRule;
    this.matchedAllocation = matchedAllocation;
    this.unmatchedAllocations = unmatchedAllocations;
    this.unevaluatedAllocations = unevaluatedAllocations;
  }

  public String getEnvironmentName() {
    return environmentName;
  }

  public FlagEvaluationCode getFlagEvaluationCode() {
    return flagEvaluationCode;
  }

  public String getFlagEvaluationDescription() {
    return flagEvaluationDescription;
  }

  public String getBanditKey() {
    return banditKey;
  }

  public String getBanditAction() {
    return banditAction;
  }

  public String getVariationKey() {
    return variationKey;
  }

  public EppoValue getVariationValue() {
    return variationValue;
  }

  public MatchedRule getMatchedRule() {
    return matchedRule;
  }

  public AllocationDetails getMatchedAllocation() {
    return matchedAllocation;
  }

  public List<AllocationDetails> getUnmatchedAllocations() {
    return unmatchedAllocations;
  }

  public List<AllocationDetails> getUnevaluatedAllocations() {
    return unevaluatedAllocations;
  }

  public boolean evaluationSuccessful() {
    return !flagEvaluationCode.isError();
  }
}

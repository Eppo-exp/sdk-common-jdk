package cloud.eppo.api;

import java.util.ArrayList;
import java.util.Date;
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
  private final Date configFetchedAt;
  private final Date configPublishedAt;

  public EvaluationDetails(
      String environmentName,
      Date configFetchedAt,
      Date configPublishedAt,
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
    this.configFetchedAt = configFetchedAt;
    this.configPublishedAt = configPublishedAt;
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

  public Date getConfigFetchedAt() {
    return configFetchedAt;
  }

  public Date getConfigPublishedAt() {
    return configPublishedAt;
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

  /**
   * Builds a default EvaluationDetails for error cases or when detailed evaluation information is
   * not available.
   *
   * @param environmentName The environment name from configuration, or null
   * @param configFetchedAt The timestamp when configuration was fetched, or null
   * @param configPublishedAt The timestamp when configuration was published, or null
   * @param code The flag evaluation code
   * @param description A description of why this evaluation result was returned
   * @param variationValue The variation value being returned
   * @return An EvaluationDetails with minimal information populated
   */
  public static EvaluationDetails buildDefault(
      String environmentName,
      Date configFetchedAt,
      Date configPublishedAt,
      FlagEvaluationCode code,
      String description,
      EppoValue variationValue) {
    return new EvaluationDetails(
        environmentName != null ? environmentName : "Unknown",
        configFetchedAt,
        configPublishedAt,
        code,
        description,
        null, // banditKey
        null, // banditAction
        null, // variationKey
        variationValue,
        null, // matchedRule
        null, // matchedAllocation
        new ArrayList<>(), // unmatchedAllocations
        new ArrayList<>()); // unevaluatedAllocations
  }
}

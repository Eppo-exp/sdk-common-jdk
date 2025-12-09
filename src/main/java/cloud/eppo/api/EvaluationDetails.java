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
  private final Date configFetchedAt;
  private final Date configPublishedAt;
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

  /** Creates a new Builder for constructing EvaluationDetails. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default EvaluationDetails for error conditions or when no flag was matched. This is a
   * convenience factory method for common error scenarios.
   */
  public static EvaluationDetails buildDefault(
      String environmentName,
      Date configFetchedAt,
      Date configPublishedAt,
      FlagEvaluationCode flagEvaluationCode,
      String flagEvaluationDescription,
      EppoValue variationValue) {
    return builder()
        .environmentName(environmentName)
        .configFetchedAt(configFetchedAt)
        .configPublishedAt(configPublishedAt)
        .flagEvaluationCode(flagEvaluationCode)
        .flagEvaluationDescription(flagEvaluationDescription)
        .variationValue(variationValue)
        .build();
  }

  /**
   * Creates a new Builder initialized with values from an existing EvaluationDetails. Useful for
   * creating a modified copy.
   */
  public static Builder builder(EvaluationDetails copyFrom) {
    return new Builder()
        .environmentName(copyFrom.environmentName)
        .configFetchedAt(copyFrom.configFetchedAt)
        .configPublishedAt(copyFrom.configPublishedAt)
        .flagEvaluationCode(copyFrom.flagEvaluationCode)
        .flagEvaluationDescription(copyFrom.flagEvaluationDescription)
        .banditKey(copyFrom.banditKey)
        .banditAction(copyFrom.banditAction)
        .variationKey(copyFrom.variationKey)
        .variationValue(copyFrom.variationValue)
        .matchedRule(copyFrom.matchedRule)
        .matchedAllocation(copyFrom.matchedAllocation)
        .unmatchedAllocations(copyFrom.unmatchedAllocations)
        .unevaluatedAllocations(copyFrom.unevaluatedAllocations);
  }

  /** Builder for constructing EvaluationDetails instances. */
  public static class Builder {
    private String environmentName = "Unknown";
    private Date configFetchedAt;
    private Date configPublishedAt;
    private FlagEvaluationCode flagEvaluationCode;
    private String flagEvaluationDescription;
    private String banditKey;
    private String banditAction;
    private String variationKey;
    private EppoValue variationValue;
    private MatchedRule matchedRule;
    private AllocationDetails matchedAllocation;
    private List<AllocationDetails> unmatchedAllocations = new ArrayList<>();
    private List<AllocationDetails> unevaluatedAllocations = new ArrayList<>();

    public Builder environmentName(String environmentName) {
      this.environmentName = environmentName != null ? environmentName : "Unknown";
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

    public Builder flagEvaluationCode(FlagEvaluationCode flagEvaluationCode) {
      this.flagEvaluationCode = flagEvaluationCode;
      return this;
    }

    public Builder flagEvaluationDescription(String flagEvaluationDescription) {
      this.flagEvaluationDescription = flagEvaluationDescription;
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

    public Builder variationKey(String variationKey) {
      this.variationKey = variationKey;
      return this;
    }

    public Builder variationValue(EppoValue variationValue) {
      this.variationValue = variationValue;
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

    public Builder unmatchedAllocations(List<AllocationDetails> unmatchedAllocations) {
      this.unmatchedAllocations =
          unmatchedAllocations != null ? new ArrayList<>(unmatchedAllocations) : new ArrayList<>();
      return this;
    }

    public Builder addUnmatchedAllocation(AllocationDetails allocation) {
      this.unmatchedAllocations.add(allocation);
      return this;
    }

    public Builder unevaluatedAllocations(List<AllocationDetails> unevaluatedAllocations) {
      this.unevaluatedAllocations =
          unevaluatedAllocations != null
              ? new ArrayList<>(unevaluatedAllocations)
              : new ArrayList<>();
      return this;
    }

    public Builder addUnevaluatedAllocation(AllocationDetails allocation) {
      this.unevaluatedAllocations.add(allocation);
      return this;
    }

    public EvaluationDetails build() {
      return new EvaluationDetails(
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
          unmatchedAllocations,
          unevaluatedAllocations);
    }
  }
}

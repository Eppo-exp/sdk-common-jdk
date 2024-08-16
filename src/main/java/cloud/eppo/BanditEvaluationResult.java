package cloud.eppo;

import cloud.eppo.api.DiscriminableAttributes;

public class BanditEvaluationResult {

  private final String flagKey;
  private final String subjectKey;
  private final DiscriminableAttributes subjectAttributes;
  private final String actionKey;
  private final DiscriminableAttributes actionAttributes;
  private final double actionScore;
  private final double actionWeight;
  private final double gamma;
  private final double optimalityGap;

  public BanditEvaluationResult(
      String flagKey,
      String subjectKey,
      DiscriminableAttributes subjectAttributes,
      String actionKey,
      DiscriminableAttributes actionAttributes,
      double actionScore,
      double actionWeight,
      double gamma,
      double optimalityGap) {
    this.flagKey = flagKey;
    this.subjectKey = subjectKey;
    this.subjectAttributes = subjectAttributes;
    this.actionKey = actionKey;
    this.actionAttributes = actionAttributes;
    this.actionScore = actionScore;
    this.actionWeight = actionWeight;
    this.gamma = gamma;
    this.optimalityGap = optimalityGap;
  }

  public String getFlagKey() {
    return flagKey;
  }

  public String getSubjectKey() {
    return subjectKey;
  }

  public DiscriminableAttributes getSubjectAttributes() {
    return subjectAttributes;
  }

  public String getActionKey() {
    return actionKey;
  }

  public DiscriminableAttributes getActionAttributes() {
    return actionAttributes;
  }

  public double getActionScore() {
    return actionScore;
  }

  public double getActionWeight() {
    return actionWeight;
  }

  public double getGamma() {
    return gamma;
  }

  public double getOptimalityGap() {
    return optimalityGap;
  }
}

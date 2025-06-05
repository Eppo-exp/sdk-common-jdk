package cloud.eppo;

import java.util.Objects;

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

  @Override
  public String toString() {
    return "BanditEvaluationResult{" +
      "flagKey='" + flagKey + '\'' +
      ", subjectKey='" + subjectKey + '\'' +
      ", subjectAttributes=" + subjectAttributes +
      ", actionKey='" + actionKey + '\'' +
      ", actionAttributes=" + actionAttributes +
      ", actionScore=" + actionScore +
      ", actionWeight=" + actionWeight +
      ", gamma=" + gamma +
      ", optimalityGap=" + optimalityGap +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BanditEvaluationResult that = (BanditEvaluationResult) o;
    return Double.compare(actionScore, that.actionScore) == 0
            && Double.compare(actionWeight, that.actionWeight) == 0
            && Double.compare(gamma, that.gamma) == 0
            && Double.compare(optimalityGap, that.optimalityGap) == 0
            && Objects.equals(flagKey, that.flagKey)
            && Objects.equals(subjectKey, that.subjectKey)
            && Objects.equals(subjectAttributes, that.subjectAttributes)
            && Objects.equals(actionKey, that.actionKey) && Objects.equals(actionAttributes, that.actionAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(flagKey, subjectKey, subjectAttributes, actionKey, actionAttributes, actionScore, actionWeight, gamma, optimalityGap);
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

package cloud.eppo.logging;

import cloud.eppo.api.Attributes;
import java.util.Date;
import java.util.Map;

public class BanditAssignment {
  private final Date timestamp;
  private final String featureFlag;
  private final String bandit;
  private final String subject;
  private final String action;
  private final Double actionProbability;
  private final Double optimalityGap;
  private final String modelVersion;
  private final Attributes subjectNumericAttributes;
  private final Attributes subjectCategoricalAttributes;
  private final Attributes actionNumericAttributes;
  private final Attributes actionCategoricalAttributes;
  private final Map<String, String> metaData;

  public BanditAssignment(
      String featureFlag,
      String bandit,
      String subject,
      String action,
      Double actionProbability,
      Double optimalityGap,
      String modelVersion,
      Attributes subjectNumericAttributes,
      Attributes subjectCategoricalAttributes,
      Attributes actionNumericAttributes,
      Attributes actionCategoricalAttributes,
      Map<String, String> metaData) {
    this.timestamp = new Date();
    this.featureFlag = featureFlag;
    this.bandit = bandit;
    this.subject = subject;
    this.action = action;
    this.actionProbability = actionProbability;
    this.optimalityGap = optimalityGap;
    this.modelVersion = modelVersion;
    this.subjectNumericAttributes = subjectNumericAttributes;
    this.subjectCategoricalAttributes = subjectCategoricalAttributes;
    this.actionNumericAttributes = actionNumericAttributes;
    this.actionCategoricalAttributes = actionCategoricalAttributes;
    this.metaData = metaData;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public String getFeatureFlag() {
    return featureFlag;
  }

  public String getBandit() {
    return bandit;
  }

  public String getSubject() {
    return subject;
  }

  public String getAction() {
    return action;
  }

  public Double getActionProbability() {
    return actionProbability;
  }

  public Double getOptimalityGap() {
    return optimalityGap;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public Attributes getSubjectNumericAttributes() {
    return subjectNumericAttributes;
  }

  public Attributes getSubjectCategoricalAttributes() {
    return subjectCategoricalAttributes;
  }

  public Attributes getActionNumericAttributes() {
    return actionNumericAttributes;
  }

  public Attributes getActionCategoricalAttributes() {
    return actionCategoricalAttributes;
  }

  public Map<String, String> getMetaData() {
    return metaData;
  }
}

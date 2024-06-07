package cloud.eppo.rac.dto;

import java.util.Date;
import java.util.Map;

/** Assignment Log Data Class */
public class BanditLogData {
  private final Date timestamp;
  private final String experiment;
  private final String banditKey;
  private final String subject;
  private final String action;
  private final Double actionProbability;
  private final String modelVersion;
  private final Map<String, Double> subjectNumericAttributes;
  private final Map<String, String> subjectCategoricalAttributes;
  private final Map<String, Double> actionNumericAttributes;
  private final Map<String, String> actionCategoricalAttributes;

  public BanditLogData(
      String experiment,
      String banditKey,
      String subject,
      String action,
      Double actionProbability,
      String modelVersion,
      Map<String, Double> subjectNumericAttributes,
      Map<String, String> subjectCategoricalAttributes,
      Map<String, Double> actionNumericAttributes,
      Map<String, String> actionCategoricalAttributes) {
    this.timestamp = new Date();
    this.experiment = experiment;
    this.banditKey = banditKey;
    this.subject = subject;
    this.action = action;
    this.actionProbability = actionProbability;
    this.modelVersion = modelVersion;
    this.subjectNumericAttributes = subjectNumericAttributes;
    this.subjectCategoricalAttributes = subjectCategoricalAttributes;
    this.actionNumericAttributes = actionNumericAttributes;
    this.actionCategoricalAttributes = actionCategoricalAttributes;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public String getExperiment() {
    return experiment;
  }

  public String getBanditKey() {
    return banditKey;
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

  public String getModelVersion() {
    return modelVersion;
  }

  public Map<String, Double> getSubjectNumericAttributes() {
    return subjectNumericAttributes;
  }

  public Map<String, String> getSubjectCategoricalAttributes() {
    return subjectCategoricalAttributes;
  }

  public Map<String, Double> getActionNumericAttributes() {
    return actionNumericAttributes;
  }

  public Map<String, String> getActionCategoricalAttributes() {
    return actionCategoricalAttributes;
  }
}

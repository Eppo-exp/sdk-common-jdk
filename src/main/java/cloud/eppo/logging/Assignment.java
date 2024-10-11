package cloud.eppo.logging;

import cloud.eppo.api.Attributes;
import java.util.Date;
import java.util.Map;

public class Assignment {
  private static final String sep = "-";

  private final Date timestamp;
  private final String experiment;
  private final String featureFlag;
  private final String allocation;
  private final String variation;
  private final String subject;
  private final Attributes subjectAttributes;
  private final Map<String, String> extraLogging;
  private final Map<String, String> metaData;

  /** An identifier key comprising the subject, flag and variation keys. */
  private final String variationKeyString;

  public Assignment(
      String experiment,
      String featureFlag,
      String allocation,
      String variation,
      String subject,
      Attributes subjectAttributes,
      Map<String, String> extraLogging,
      Map<String, String> metaData) {
    this.timestamp = new Date();
    this.experiment = experiment;
    this.featureFlag = featureFlag;
    this.allocation = allocation;
    this.variation = variation;
    this.subject = subject;
    this.subjectAttributes = subjectAttributes;
    this.extraLogging = extraLogging;
    this.metaData = metaData;

    variationKeyString = subject + sep + featureFlag + sep + variation;
  }

  public String getIdentifier() {
    return variationKeyString;
  }

  public String getExperiment() {
    return experiment;
  }

  public String getFeatureFlag() {
    return featureFlag;
  }

  public String getAllocation() {
    return allocation;
  }

  public String getVariation() {
    return variation;
  }

  public String getSubject() {
    return subject;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public Attributes getSubjectAttributes() {
    return subjectAttributes;
  }

  public Map<String, String> getExtraLogging() {
    return extraLogging;
  }

  public Map<String, String> getMetaData() {
    return metaData;
  }

  @Override
  public String toString() {
    return "Subject "
        + subject
        + " assigned to variation "
        + variation
        + " in experiment "
        + experiment;
  }
}

package cloud.eppo.logging;

import cloud.eppo.Utils;
import cloud.eppo.ufc.dto.Attributes;
import java.util.Date;
import java.util.Map;

public class Assignment {
  private final Date timestamp;
  private final String experiment;
  private final String featureFlag;
  private final String allocation;
  private final String variation;
  private final String subject;
  private final Attributes attributes;
  private final Map<String, String> extraLogging;
  private final Map<String, String> metaData;

  public Assignment(
    String experiment,
    String featureFlag,
    String allocation,
    String variation,
    String subject,
    Attributes attributes,
    Map<String, String> extraLogging,
    Map<String, String> metaData) {
    this.timestamp = new Date();
    this.experiment = experiment;
    this.featureFlag = featureFlag;
    this.allocation = allocation;
    this.variation = variation;
    this.subject = subject;
    this.attributes = attributes;
    this.extraLogging = extraLogging;
    this.metaData = metaData;
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
    return attributes;
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

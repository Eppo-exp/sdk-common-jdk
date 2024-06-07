package cloud.eppo.rac.dto;

import java.util.Date;

/** Assignment Log Data Class */
public class AssignmentLogData {
  private final String experiment;
  private final String featureFlag;
  private final String allocation;
  private final String variation;
  private final Date timestamp;
  private final String subject;
  private final EppoAttributes subjectAttributes;

  public AssignmentLogData(
      String experiment,
      String featureFlag,
      String allocation,
      String variation,
      String subject,
      EppoAttributes subjectAttributes) {
    this.experiment = experiment;
    this.featureFlag = featureFlag;
    this.allocation = allocation;
    this.variation = variation;
    this.timestamp = new Date();
    this.subject = subject;
    this.subjectAttributes = subjectAttributes;
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

  public Date getTimestamp() {
    return timestamp;
  }

  public String getSubject() {
    return subject;
  }

  public EppoAttributes getSubjectAttributes() {
    return subjectAttributes;
  }
}

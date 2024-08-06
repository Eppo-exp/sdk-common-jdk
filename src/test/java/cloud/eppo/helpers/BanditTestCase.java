package cloud.eppo.helpers;

import java.util.List;

public class BanditTestCase {
  private final String flag;
  private final String defaultValue;
  private final List<BanditSubjectAssignment> subjects;
  private String fileName;

  public BanditTestCase(String flag, String defaultValue, List<BanditSubjectAssignment> subjects) {
    this.flag = flag;
    this.defaultValue = defaultValue;
    this.subjects = subjects;
  }

  public String getFlag() {
    return flag;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public List<BanditSubjectAssignment> getSubjects() {
    return subjects;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileName() {
    return fileName;
  }
}

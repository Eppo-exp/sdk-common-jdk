package cloud.eppo.rac.dto;

import cloud.eppo.rac.Constants;

/** Eppo Client Config class */
public class EppoClientConfig {
  private final String apiKey;
  private final String baseURL = Constants.DEFAULT_BASE_URL;
  private final IAssignmentLogger assignmentLogger;
  private final IBanditLogger banditLogger;

  /** When set to true, the client will not throw an exception when it encounters an error. */
  private boolean isGracefulMode = true;

  public EppoClientConfig(
      String apiKey, IAssignmentLogger assignmentLogger, IBanditLogger banditLogger) {
    this.apiKey = apiKey;
    this.assignmentLogger = assignmentLogger;
    this.banditLogger = banditLogger;
  }

  public boolean isGracefulMode() {
    return isGracefulMode;
  }

  public void setGracefulMode(boolean gracefulMode) {
    isGracefulMode = gracefulMode;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getBaseURL() {
    return baseURL;
  }

  public IAssignmentLogger getAssignmentLogger() {
    return assignmentLogger;
  }

  public IBanditLogger getBanditLogger() {
    return banditLogger;
  }
}

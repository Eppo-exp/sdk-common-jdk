package cloud.eppo.rac.dto;

/** Eppo Client Config class */
public class EppoClientConfig {
  private final String apiKey;
  private final String baseUrl;
  private final IAssignmentLogger assignmentLogger;
  private final IBanditLogger banditLogger;

  /** When set to true, the client will not throw an exception when it encounters an error. */
  private boolean isGracefulMode = true;

  public EppoClientConfig(
      String apiKey,
      String baseUrl,
      IAssignmentLogger assignmentLogger,
      IBanditLogger banditLogger) {
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
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

  public String getBaseUrl() {
    return baseUrl;
  }

  public IAssignmentLogger getAssignmentLogger() {
    return assignmentLogger;
  }

  public IBanditLogger getBanditLogger() {
    return banditLogger;
  }
}

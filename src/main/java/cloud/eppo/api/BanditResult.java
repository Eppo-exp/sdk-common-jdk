package cloud.eppo.api;

public class BanditResult {
  private final String variation;
  private final String action;

  public BanditResult(String variation, String action) {
    this.variation = variation;
    this.action = action;
  }

  public String getVariation() {
    return variation;
  }

  public String getAction() {
    return action;
  }
}

package cloud.eppo.api;

import java.util.Objects;

public class BanditResult {
  private final String variation;
  private final String action;

  public BanditResult(String variation, String action) {
    this.variation = variation;
    this.action = action;
  }

  @Override
  public String toString() {
    return "BanditResult{" +
      "variation='" + variation + '\'' +
      ", action='" + action + '\'' +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BanditResult that = (BanditResult) o;
    return Objects.equals(variation, that.variation)
      && Objects.equals(action, that.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(variation, action);
  }

  public String getVariation() {
    return variation;
  }

  public String getAction() {
    return action;
  }
}

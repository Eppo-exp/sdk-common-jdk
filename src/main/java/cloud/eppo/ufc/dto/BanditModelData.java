package cloud.eppo.ufc.dto;

import cloud.eppo.api.IBanditModelData;
import java.util.Map;
import java.util.Objects;

public class BanditModelData implements IBanditModelData {
  private final Double gamma;
  private final Double defaultActionScore;
  private final Double actionProbabilityFloor;
  private final Map<String, BanditCoefficients> coefficients;

  public BanditModelData(
      Double gamma,
      Double defaultActionScore,
      Double actionProbabilityFloor,
      Map<String, BanditCoefficients> coefficients) {
    this.gamma = gamma;
    this.defaultActionScore = defaultActionScore;
    this.actionProbabilityFloor = actionProbabilityFloor;
    this.coefficients = coefficients;
  }

  @Override
  public String toString() {
    return "BanditModelData{" +
      "gamma=" + gamma +
      ", defaultActionScore=" + defaultActionScore +
      ", actionProbabilityFloor=" + actionProbabilityFloor +
      ", coefficients=" + coefficients +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BanditModelData that = (BanditModelData) o;
    return Objects.equals(gamma, that.gamma)
            && Objects.equals(defaultActionScore, that.defaultActionScore)
            && Objects.equals(actionProbabilityFloor, that.actionProbabilityFloor)
            && Objects.equals(coefficients, that.coefficients);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gamma, defaultActionScore, actionProbabilityFloor, coefficients);
  }

  public Double getGamma() {
    return gamma;
  }

  public Double getDefaultActionScore() {
    return defaultActionScore;
  }

  public Double getActionProbabilityFloor() {
    return actionProbabilityFloor;
  }

  public Map<String, BanditCoefficients> getCoefficients() {
    return coefficients;
  }
}

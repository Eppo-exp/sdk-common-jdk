package cloud.eppo.rac.dto;

import java.util.Map;

public class BanditModelData {
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

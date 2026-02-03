package cloud.eppo.api.dto;

import java.util.Map;
import java.util.Objects;

public interface BanditModelData {
  Double getGamma();

  Double getDefaultActionScore();

  Double getActionProbabilityFloor();

  Map<String, BanditCoefficients> getCoefficients();

  class Default implements BanditModelData {
    private final Double gamma;
    private final Double defaultActionScore;
    private final Double actionProbabilityFloor;
    private final Map<String, BanditCoefficients> coefficients;

    public Default(
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
      return "BanditModelData{"
          + "gamma="
          + gamma
          + ", defaultActionScore="
          + defaultActionScore
          + ", actionProbabilityFloor="
          + actionProbabilityFloor
          + ", coefficients="
          + coefficients
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      BanditModelData that = (BanditModelData) o;
      return Objects.equals(gamma, that.getGamma())
          && Objects.equals(defaultActionScore, that.getDefaultActionScore())
          && Objects.equals(actionProbabilityFloor, that.getActionProbabilityFloor())
          && Objects.equals(coefficients, that.getCoefficients());
    }

    @Override
    public int hashCode() {
      return Objects.hash(gamma, defaultActionScore, actionProbabilityFloor, coefficients);
    }

    @Override
    public Double getGamma() {
      return gamma;
    }

    @Override
    public Double getDefaultActionScore() {
      return defaultActionScore;
    }

    @Override
    public Double getActionProbabilityFloor() {
      return actionProbabilityFloor;
    }

    @Override
    public Map<String, BanditCoefficients> getCoefficients() {
      return coefficients;
    }
  }
}

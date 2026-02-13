package cloud.eppo.api.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BanditModelData extends Serializable {
  @NotNull Double getGamma();

  @NotNull Double getDefaultActionScore();

  @NotNull Double getActionProbabilityFloor();

  @NotNull Map<String, BanditCoefficients> getCoefficients();

  class Default implements BanditModelData {
    private static final long serialVersionUID = 1L;
    private final @NotNull Double gamma;
    private final @NotNull Double defaultActionScore;
    private final @NotNull Double actionProbabilityFloor;
    private final @NotNull Map<String, BanditCoefficients> coefficients;

    public Default(
        @NotNull Double gamma,
        @NotNull Double defaultActionScore,
        @NotNull Double actionProbabilityFloor,
        @Nullable Map<String, BanditCoefficients> coefficients) {
      this.gamma = gamma;
      this.defaultActionScore = defaultActionScore;
      this.actionProbabilityFloor = actionProbabilityFloor;
      this.coefficients = coefficients == null
          ? Collections.emptyMap()
          : Collections.unmodifiableMap(new HashMap<>(coefficients));
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
    @NotNull
    public Double getGamma() {
      return gamma;
    }

    @Override
    @NotNull
    public Double getDefaultActionScore() {
      return defaultActionScore;
    }

    @Override
    @NotNull
    public Double getActionProbabilityFloor() {
      return actionProbabilityFloor;
    }

    @Override
    @NotNull
    public Map<String, BanditCoefficients> getCoefficients() {
      return coefficients;
    }
  }
}

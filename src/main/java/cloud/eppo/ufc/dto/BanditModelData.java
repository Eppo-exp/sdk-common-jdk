package cloud.eppo.ufc.dto;

import java.util.HashMap;
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

  @SuppressWarnings("unchecked")
  public BanditModelData(Map<String, Object> modelData) {
    this.gamma = (Double) modelData.getOrDefault("gamma", 1.0);
    this.defaultActionScore = (Double) modelData.getOrDefault("defaultActionScore", 0.0);
    this.actionProbabilityFloor = (Double) modelData.getOrDefault("actionProbabilityFloor", 0.0);

    // For now, just use an empty map for coefficients
    // In a real implementation, we would need to convert the raw coefficient data
    // into proper BanditCoefficients objects
    this.coefficients = new HashMap<>();

    // Extract coefficients if they exist
    if (modelData.containsKey("coefficients") && modelData.get("coefficients") instanceof Map) {
      Map<String, Object> rawCoefficients = (Map<String, Object>) modelData.get("coefficients");
      for (Map.Entry<String, Object> entry : rawCoefficients.entrySet()) {
        if (entry.getValue() instanceof Map) {
          // Convert raw coefficient data to BanditCoefficients
          // This is a simplified version and might need more detailed conversion
          coefficients.put(
              entry.getKey(),
              new BanditCoefficients(entry.getKey(), (Map<String, Object>) entry.getValue()));
        }
      }
    }
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

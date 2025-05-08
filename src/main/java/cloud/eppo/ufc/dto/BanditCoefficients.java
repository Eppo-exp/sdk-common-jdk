package cloud.eppo.ufc.dto;

import java.util.HashMap;
import java.util.Map;

public class BanditCoefficients {
  private final String actionKey;
  private final Double intercept;
  private final Map<String, BanditNumericAttributeCoefficients> subjectNumericCoefficients;
  private final Map<String, BanditCategoricalAttributeCoefficients> subjectCategoricalCoefficients;
  private final Map<String, BanditNumericAttributeCoefficients> actionNumericCoefficients;
  private final Map<String, BanditCategoricalAttributeCoefficients> actionCategoricalCoefficients;

  public BanditCoefficients(
      String actionKey,
      Double intercept,
      Map<String, BanditNumericAttributeCoefficients> subjectNumericAttributeCoefficients,
      Map<String, BanditCategoricalAttributeCoefficients> subjectCategoricalAttributeCoefficients,
      Map<String, BanditNumericAttributeCoefficients> actionNumericAttributeCoefficients,
      Map<String, BanditCategoricalAttributeCoefficients> actionCategoricalAttributeCoefficients) {
    this.actionKey = actionKey;
    this.intercept = intercept;
    this.subjectNumericCoefficients = subjectNumericAttributeCoefficients;
    this.subjectCategoricalCoefficients = subjectCategoricalAttributeCoefficients;
    this.actionNumericCoefficients = actionNumericAttributeCoefficients;
    this.actionCategoricalCoefficients = actionCategoricalAttributeCoefficients;
  }

  @SuppressWarnings("unchecked")
  public BanditCoefficients(String actionKey, Map<String, Object> data) {
    this.actionKey = actionKey;
    this.intercept = (Double) data.getOrDefault("intercept", 0.0);

    // Initialize empty maps for coefficients
    this.subjectNumericCoefficients = new HashMap<>();
    this.subjectCategoricalCoefficients = new HashMap<>();
    this.actionNumericCoefficients = new HashMap<>();
    this.actionCategoricalCoefficients = new HashMap<>();

    // In a real implementation, we would need to convert the raw coefficient data
    // This is a simplified version for compilation purposes
  }

  public String getActionKey() {
    return actionKey;
  }

  public Double getIntercept() {
    return intercept;
  }

  public Map<String, BanditNumericAttributeCoefficients> getSubjectNumericCoefficients() {
    return subjectNumericCoefficients;
  }

  public Map<String, BanditCategoricalAttributeCoefficients> getSubjectCategoricalCoefficients() {
    return subjectCategoricalCoefficients;
  }

  public Map<String, BanditNumericAttributeCoefficients> getActionNumericCoefficients() {
    return actionNumericCoefficients;
  }

  public Map<String, BanditCategoricalAttributeCoefficients> getActionCategoricalCoefficients() {
    return actionCategoricalCoefficients;
  }
}

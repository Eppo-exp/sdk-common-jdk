package cloud.eppo.ufc.dto;

import cloud.eppo.api.IBanditCoefficients;
import java.util.Map;
import java.util.Objects;

public class BanditCoefficients implements IBanditCoefficients {
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

  @Override
  public String toString() {
    return "BanditCoefficients{"
        + "actionKey='"
        + actionKey
        + '\''
        + ", intercept="
        + intercept
        + ", subjectNumericCoefficients="
        + subjectNumericCoefficients
        + ", subjectCategoricalCoefficients="
        + subjectCategoricalCoefficients
        + ", actionNumericCoefficients="
        + actionNumericCoefficients
        + ", actionCategoricalCoefficients="
        + actionCategoricalCoefficients
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BanditCoefficients that = (BanditCoefficients) o;
    return Objects.equals(actionKey, that.actionKey)
        && Objects.equals(intercept, that.intercept)
        && Objects.equals(subjectNumericCoefficients, that.subjectNumericCoefficients)
        && Objects.equals(subjectCategoricalCoefficients, that.subjectCategoricalCoefficients)
        && Objects.equals(actionNumericCoefficients, that.actionNumericCoefficients)
        && Objects.equals(actionCategoricalCoefficients, that.actionCategoricalCoefficients);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        actionKey,
        intercept,
        subjectNumericCoefficients,
        subjectCategoricalCoefficients,
        actionNumericCoefficients,
        actionCategoricalCoefficients);
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

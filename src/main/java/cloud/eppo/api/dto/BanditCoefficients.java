package cloud.eppo.api.dto;

import cloud.eppo.ufc.dto.BanditCategoricalAttributeCoefficients;
import cloud.eppo.ufc.dto.BanditNumericAttributeCoefficients;
import java.util.Map;
import java.util.Objects;

public interface BanditCoefficients {
  String getActionKey();

  Double getIntercept();

  Map<String, BanditNumericAttributeCoefficients> getSubjectNumericCoefficients();

  Map<String, BanditCategoricalAttributeCoefficients> getSubjectCategoricalCoefficients();

  Map<String, BanditNumericAttributeCoefficients> getActionNumericCoefficients();

  Map<String, BanditCategoricalAttributeCoefficients> getActionCategoricalCoefficients();

  class Default implements BanditCoefficients {
    private final String actionKey;
    private final Double intercept;
    private final Map<String, BanditNumericAttributeCoefficients> subjectNumericCoefficients;
    private final Map<String, BanditCategoricalAttributeCoefficients>
        subjectCategoricalCoefficients;
    private final Map<String, BanditNumericAttributeCoefficients> actionNumericCoefficients;
    private final Map<String, BanditCategoricalAttributeCoefficients> actionCategoricalCoefficients;

    public Default(
        String actionKey,
        Double intercept,
        Map<String, BanditNumericAttributeCoefficients> subjectNumericAttributeCoefficients,
        Map<String, BanditCategoricalAttributeCoefficients> subjectCategoricalAttributeCoefficients,
        Map<String, BanditNumericAttributeCoefficients> actionNumericAttributeCoefficients,
        Map<String, BanditCategoricalAttributeCoefficients>
            actionCategoricalAttributeCoefficients) {
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
      return Objects.equals(actionKey, that.getActionKey())
          && Objects.equals(intercept, that.getIntercept())
          && Objects.equals(subjectNumericCoefficients, that.getSubjectNumericCoefficients())
          && Objects.equals(
              subjectCategoricalCoefficients, that.getSubjectCategoricalCoefficients())
          && Objects.equals(actionNumericCoefficients, that.getActionNumericCoefficients())
          && Objects.equals(actionCategoricalCoefficients, that.getActionCategoricalCoefficients());
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

    @Override
    public String getActionKey() {
      return actionKey;
    }

    @Override
    public Double getIntercept() {
      return intercept;
    }

    @Override
    public Map<String, BanditNumericAttributeCoefficients> getSubjectNumericCoefficients() {
      return subjectNumericCoefficients;
    }

    @Override
    public Map<String, BanditCategoricalAttributeCoefficients> getSubjectCategoricalCoefficients() {
      return subjectCategoricalCoefficients;
    }

    @Override
    public Map<String, BanditNumericAttributeCoefficients> getActionNumericCoefficients() {
      return actionNumericCoefficients;
    }

    @Override
    public Map<String, BanditCategoricalAttributeCoefficients> getActionCategoricalCoefficients() {
      return actionCategoricalCoefficients;
    }
  }
}

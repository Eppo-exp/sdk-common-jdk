package cloud.eppo.api.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BanditCoefficients extends Serializable {
  @NotNull String getActionKey();

  @NotNull Double getIntercept();

  @NotNull Map<String, BanditNumericAttributeCoefficients> getSubjectNumericCoefficients();

  @NotNull Map<String, BanditCategoricalAttributeCoefficients> getSubjectCategoricalCoefficients();

  @NotNull Map<String, BanditNumericAttributeCoefficients> getActionNumericCoefficients();

  @NotNull Map<String, BanditCategoricalAttributeCoefficients> getActionCategoricalCoefficients();

  class Default implements BanditCoefficients {
    private static final long serialVersionUID = 1L;
    private final @NotNull String actionKey;
    private final @NotNull Double intercept;
    private final @NotNull Map<String, BanditNumericAttributeCoefficients>
        subjectNumericCoefficients;
    private final @NotNull Map<String, BanditCategoricalAttributeCoefficients>
        subjectCategoricalCoefficients;
    private final @NotNull Map<String, BanditNumericAttributeCoefficients>
        actionNumericCoefficients;
    private final @NotNull Map<String, BanditCategoricalAttributeCoefficients>
        actionCategoricalCoefficients;

    public Default(
        @NotNull String actionKey,
        @NotNull Double intercept,
        @Nullable Map<String, BanditNumericAttributeCoefficients> subjectNumericAttributeCoefficients,
        @Nullable Map<String, BanditCategoricalAttributeCoefficients>
                subjectCategoricalAttributeCoefficients,
        @Nullable Map<String, BanditNumericAttributeCoefficients> actionNumericAttributeCoefficients,
        @Nullable Map<String, BanditCategoricalAttributeCoefficients>
                actionCategoricalAttributeCoefficients) {
      this.actionKey = actionKey;
      this.intercept = intercept;
      this.subjectNumericCoefficients =
          subjectNumericAttributeCoefficients == null
              ? Collections.emptyMap()
              : Collections.unmodifiableMap(new HashMap<>(subjectNumericAttributeCoefficients));
      this.subjectCategoricalCoefficients =
          subjectCategoricalAttributeCoefficients == null
              ? Collections.emptyMap()
              : Collections.unmodifiableMap(new HashMap<>(subjectCategoricalAttributeCoefficients));
      this.actionNumericCoefficients =
          actionNumericAttributeCoefficients == null
              ? Collections.emptyMap()
              : Collections.unmodifiableMap(new HashMap<>(actionNumericAttributeCoefficients));
      this.actionCategoricalCoefficients =
          actionCategoricalAttributeCoefficients == null
              ? Collections.emptyMap()
              : Collections.unmodifiableMap(new HashMap<>(actionCategoricalAttributeCoefficients));
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
    @NotNull public String getActionKey() {
      return actionKey;
    }

    @Override
    @NotNull public Double getIntercept() {
      return intercept;
    }

    @Override
    @NotNull public Map<String, BanditNumericAttributeCoefficients> getSubjectNumericCoefficients() {
      return subjectNumericCoefficients;
    }

    @Override
    @NotNull public Map<String, BanditCategoricalAttributeCoefficients> getSubjectCategoricalCoefficients() {
      return subjectCategoricalCoefficients;
    }

    @Override
    @NotNull public Map<String, BanditNumericAttributeCoefficients> getActionNumericCoefficients() {
      return actionNumericCoefficients;
    }

    @Override
    @NotNull public Map<String, BanditCategoricalAttributeCoefficients> getActionCategoricalCoefficients() {
      return actionCategoricalCoefficients;
    }
  }
}

package cloud.eppo.api.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BanditCategoricalAttributeCoefficients extends BanditAttributeCoefficients {
  @NotNull Double getMissingValueCoefficient();

  @NotNull Map<String, Double> getValueCoefficients();

  class Default implements BanditCategoricalAttributeCoefficients {
    private static final long serialVersionUID = 1L;
    private final @NotNull String attributeKey;
    private final @NotNull Double missingValueCoefficient;
    private final @NotNull Map<String, Double> valueCoefficients;

    public Default(
        @NotNull String attributeKey,
        @NotNull Double missingValueCoefficient,
        @Nullable Map<String, Double> valueCoefficients) {
      this.attributeKey = attributeKey;
      this.missingValueCoefficient = missingValueCoefficient;
      this.valueCoefficients = valueCoefficients == null
          ? Collections.emptyMap()
          : Collections.unmodifiableMap(new HashMap<>(valueCoefficients));
    }

    @Override
    public String toString() {
      return "BanditCategoricalAttributeCoefficients{"
          + "attributeKey='"
          + attributeKey
          + '\''
          + ", missingValueCoefficient="
          + missingValueCoefficient
          + ", valueCoefficients="
          + valueCoefficients
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      BanditCategoricalAttributeCoefficients that = (BanditCategoricalAttributeCoefficients) o;
      return Objects.equals(attributeKey, that.getAttributeKey())
          && Objects.equals(missingValueCoefficient, that.getMissingValueCoefficient())
          && Objects.equals(valueCoefficients, that.getValueCoefficients());
    }

    @Override
    public int hashCode() {
      return Objects.hash(attributeKey, missingValueCoefficient, valueCoefficients);
    }

    @Override
    @NotNull
    public String getAttributeKey() {
      return attributeKey;
    }

    @Override
    @NotNull
    public Double getMissingValueCoefficient() {
      return missingValueCoefficient;
    }

    @Override
    @NotNull
    public Map<String, Double> getValueCoefficients() {
      return valueCoefficients;
    }
  }
}

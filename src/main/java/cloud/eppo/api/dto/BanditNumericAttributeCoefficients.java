package cloud.eppo.api.dto;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public interface BanditNumericAttributeCoefficients extends BanditAttributeCoefficients {
  @NotNull Double getCoefficient();

  @NotNull Double getMissingValueCoefficient();

  class Default implements BanditNumericAttributeCoefficients {
    private static final long serialVersionUID = 1L;
    private final String attributeKey;
    private final Double coefficient;
    private final Double missingValueCoefficient;

    public Default(String attributeKey, Double coefficient, Double missingValueCoefficient) {
      this.attributeKey = attributeKey;
      this.coefficient = coefficient;
      this.missingValueCoefficient = missingValueCoefficient;
    }

    @Override
    public String toString() {
      return "BanditNumericAttributeCoefficients{"
          + "attributeKey='"
          + attributeKey
          + '\''
          + ", coefficient="
          + coefficient
          + ", missingValueCoefficient="
          + missingValueCoefficient
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      BanditNumericAttributeCoefficients that = (BanditNumericAttributeCoefficients) o;
      return Objects.equals(attributeKey, that.getAttributeKey())
          && Objects.equals(coefficient, that.getCoefficient())
          && Objects.equals(missingValueCoefficient, that.getMissingValueCoefficient());
    }

    @Override
    public int hashCode() {
      return Objects.hash(attributeKey, coefficient, missingValueCoefficient);
    }

    @Override
    public String getAttributeKey() {
      return attributeKey;
    }

    @Override
    public Double getCoefficient() {
      return coefficient;
    }

    @Override
    public Double getMissingValueCoefficient() {
      return missingValueCoefficient;
    }
  }
}

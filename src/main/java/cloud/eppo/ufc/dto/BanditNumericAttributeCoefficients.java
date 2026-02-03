package cloud.eppo.ufc.dto;

import cloud.eppo.api.EppoValue;
import cloud.eppo.api.dto.BanditAttributeCoefficients;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BanditNumericAttributeCoefficients implements BanditAttributeCoefficients {
  private static final Logger logger =
      LoggerFactory.getLogger(BanditNumericAttributeCoefficients.class);
  private final String attributeKey;
  private final Double coefficient;
  private final Double missingValueCoefficient;

  public BanditNumericAttributeCoefficients(
      String attributeKey, Double coefficient, Double missingValueCoefficient) {
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
  public double scoreForAttributeValue(EppoValue attributeValue) {
    if (attributeValue == null || attributeValue.isNull()) {
      return missingValueCoefficient;
    }
    if (!attributeValue.isNumeric()) {
      logger.warn("Unexpected categorical attribute value for attribute {}", attributeKey);
    }
    return coefficient * attributeValue.doubleValue();
  }

  public Double getCoefficient() {
    return coefficient;
  }

  public Double getMissingValueCoefficient() {
    return missingValueCoefficient;
  }
}

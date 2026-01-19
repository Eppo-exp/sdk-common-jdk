package cloud.eppo.ufc.dto;

import cloud.eppo.api.EppoValue;
import cloud.eppo.api.IBanditNumericAttributeCoefficients;
import cloud.eppo.api.IEppoValue;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BanditNumericAttributeCoefficients
    implements IBanditNumericAttributeCoefficients, BanditAttributeCoefficients {
  private final Logger logger = LoggerFactory.getLogger(BanditNumericAttributeCoefficients.class);
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
    return Objects.equals(logger, that.logger)
        && Objects.equals(attributeKey, that.attributeKey)
        && Objects.equals(coefficient, that.coefficient)
        && Objects.equals(missingValueCoefficient, that.missingValueCoefficient);
  }

  @Override
  public int hashCode() {
    return Objects.hash(logger, attributeKey, coefficient, missingValueCoefficient);
  }

  @Override
  public String getAttributeKey() {
    return attributeKey;
  }

  @Override
  public double scoreForAttributeValue(IEppoValue attributeValue) {
    if (attributeValue == null || attributeValue.isNull()) {
      return missingValueCoefficient;
    }
    if (!attributeValue.isNumeric()) {
      logger.warn("Unexpected categorical attribute value for attribute {}", attributeKey);
    }
    return coefficient * attributeValue.doubleValue();
  }

  // For backward compatibility with BanditAttributeCoefficients interface
  public double scoreForAttributeValue(EppoValue attributeValue) {
    return scoreForAttributeValue((IEppoValue) attributeValue);
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

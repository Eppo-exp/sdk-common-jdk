package cloud.eppo.ufc.dto;

import cloud.eppo.api.EppoValue;
import cloud.eppo.api.dto.BanditAttributeCoefficients;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BanditCategoricalAttributeCoefficients implements BanditAttributeCoefficients {
  private static final Logger logger =
      LoggerFactory.getLogger(BanditCategoricalAttributeCoefficients.class);
  private final String attributeKey;
  private final Double missingValueCoefficient;
  private final Map<String, Double> valueCoefficients;

  public BanditCategoricalAttributeCoefficients(
      String attributeKey, Double missingValueCoefficient, Map<String, Double> valueCoefficients) {
    this.attributeKey = attributeKey;
    this.missingValueCoefficient = missingValueCoefficient;
    this.valueCoefficients = valueCoefficients;
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
  public String getAttributeKey() {
    return attributeKey;
  }

  @Override
  public double scoreForAttributeValue(EppoValue attributeValue) {
    if (attributeValue == null || attributeValue.isNull()) {
      return missingValueCoefficient;
    }
    if (attributeValue.isNumeric()) {
      logger.warn("Unexpected numeric attribute value for attribute {}", attributeKey);
      return missingValueCoefficient;
    }

    String valueKey = attributeValue.toString();
    Double coefficient = valueCoefficients.get(valueKey);

    // Categorical attributes are treated as one-hot booleans, so it's just the coefficient * 1
    // when present
    return coefficient != null ? coefficient : missingValueCoefficient;
  }

  public Double getMissingValueCoefficient() {
    return missingValueCoefficient;
  }

  public Map<String, Double> getValueCoefficients() {
    return valueCoefficients;
  }
}

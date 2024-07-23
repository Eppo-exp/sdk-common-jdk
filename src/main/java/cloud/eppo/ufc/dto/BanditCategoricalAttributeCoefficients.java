package cloud.eppo.ufc.dto;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BanditCategoricalAttributeCoefficients implements BanditAttributeCoefficients {
  private final Logger logger =
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
  public String getAttributeKey() {
    return attributeKey;
  }

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

    // Categorical attributes are treated as one-hot booleans, so it's just the coefficient * 1 when
    // present
    return coefficient != null ? coefficient : missingValueCoefficient;
  }

  public Double getMissingValueCoefficient() {
    return missingValueCoefficient;
  }

  public Map<String, Double> getValueCoefficients() {
    return valueCoefficients;
  }
}

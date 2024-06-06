package cloud.eppo.rac.dto;

public class BanditNumericAttributeCoefficients implements AttributeCoefficients {
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
  public double scoreForAttributeValue(EppoValue attributeValue) {
    if (attributeValue == null || attributeValue.isNull()) {
      return missingValueCoefficient;
    }
    if (!attributeValue.isNumeric()) {
      log.warn("Unexpected categorical attribute value for attribute " + attributeKey);
    }
    return coefficient * attributeValue.doubleValue();
  }

  @Override
  public String getAttributeKey() {
    return attributeKey;
  }

  public Double getCoefficient() {
    return coefficient;
  }

  public Double getMissingValueCoefficient() {
    return missingValueCoefficient;
  }
}

package cloud.eppo.rac.dto;

public interface AttributeCoefficients {
  String getAttributeKey();

  double scoreForAttributeValue(EppoValue attributeValue);
}

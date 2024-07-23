package cloud.eppo.ufc.dto;

public interface BanditAttributeCoefficients {

  String getAttributeKey();
  double scoreForAttributeValue(EppoValue attributeValue);
}

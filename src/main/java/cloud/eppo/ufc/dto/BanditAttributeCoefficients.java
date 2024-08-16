package cloud.eppo.ufc.dto;

import cloud.eppo.api.EppoValue;

public interface BanditAttributeCoefficients {

  String getAttributeKey();

  double scoreForAttributeValue(EppoValue attributeValue);
}

package cloud.eppo.ufc.dto;

import cloud.eppo.api.EppoValue;
import cloud.eppo.api.IBanditAttributeCoefficients;

public interface BanditAttributeCoefficients extends IBanditAttributeCoefficients {

  String getAttributeKey();

  double scoreForAttributeValue(EppoValue attributeValue);
}

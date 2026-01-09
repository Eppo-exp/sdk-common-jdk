package cloud.eppo.ufc.dto;

import cloud.eppo.api.IBanditAttributeCoefficients;
import cloud.eppo.api.IEppoValue;

public interface BanditAttributeCoefficients extends IBanditAttributeCoefficients {

  String getAttributeKey();

  double scoreForAttributeValue(IEppoValue attributeValue);
}

package cloud.eppo.api.dto;

import cloud.eppo.api.EppoValue;
import org.jetbrains.annotations.NotNull;

public interface BanditAttributeCoefficients {

  @NotNull String getAttributeKey();

  double scoreForAttributeValue(@NotNull EppoValue attributeValue);
}

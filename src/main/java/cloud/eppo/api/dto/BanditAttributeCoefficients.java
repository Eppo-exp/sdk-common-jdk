package cloud.eppo.api.dto;

import java.io.Serializable;
import org.jetbrains.annotations.NotNull;

public interface BanditAttributeCoefficients extends Serializable {

  @NotNull String getAttributeKey();
}

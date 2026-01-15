package cloud.eppo.api;

import java.util.Map;

/** Interface for BanditModelData allowing downstream SDKs to provide custom implementations. */
public interface IBanditModelData {
  Double getGamma();

  Double getDefaultActionScore();

  Double getActionProbabilityFloor();

  Map<String, ? extends IBanditCoefficients> getCoefficients();
}

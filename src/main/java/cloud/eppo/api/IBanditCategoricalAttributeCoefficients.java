package cloud.eppo.api;

import java.util.Map;

/**
 * Interface for BanditCategoricalAttributeCoefficients allowing downstream SDKs to provide custom implementations.
 */
public interface IBanditCategoricalAttributeCoefficients extends IBanditAttributeCoefficients {
  Double getMissingValueCoefficient();

  Map<String, Double> getValueCoefficients();
}

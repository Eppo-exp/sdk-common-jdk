package cloud.eppo.api;

/**
 * Interface for BanditNumericAttributeCoefficients allowing downstream SDKs to provide custom
 * implementations.
 */
public interface IBanditNumericAttributeCoefficients extends IBanditAttributeCoefficients {
  Double getCoefficient();

  Double getMissingValueCoefficient();
}

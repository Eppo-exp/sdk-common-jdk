package cloud.eppo.api;

/**
 * Interface for BanditAttributeCoefficients allowing downstream SDKs to provide custom
 * implementations.
 */
public interface IBanditAttributeCoefficients {
  String getAttributeKey();

  double scoreForAttributeValue(IEppoValue attributeValue);
}

package cloud.eppo.api;

/**
 * Interface for BanditFlagVariation allowing downstream SDKs to provide custom implementations.
 */
public interface IBanditFlagVariation {
  String getBanditKey();

  String getFlagKey();

  String getAllocationKey();

  String getVariationKey();

  String getVariationValue();
}

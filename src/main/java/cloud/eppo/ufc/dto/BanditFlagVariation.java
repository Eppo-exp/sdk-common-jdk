package cloud.eppo.ufc.dto;

public class BanditFlagVariation {
  private final String banditKey;
  private final String flagKey;
  private final String allocationKey;
  private final String variationKey;
  private final String variationValue;

  public BanditFlagVariation(
      String banditKey,
      String flagKey,
      String allocationKey,
      String variationKey,
      String variationValue
  ) {
    this.banditKey = banditKey;
    this.flagKey = flagKey;
    this.allocationKey = allocationKey;
    this.variationKey = variationKey;
    this.variationValue = variationValue;
  }

  public String getBanditKey() {
    return banditKey;
  }

  public String getFlagKey() {
    return flagKey;
  }

  public String getAllocationKey() {
    return allocationKey;
  }

  public String getVariationKey() {
    return variationKey;
  }

  public String getVariationValue() {
    return variationValue;
  }
}

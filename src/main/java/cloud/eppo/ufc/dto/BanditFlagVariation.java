package cloud.eppo.ufc.dto;

import cloud.eppo.api.IBanditFlagVariation;
import java.util.Objects;

public class BanditFlagVariation implements IBanditFlagVariation {
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
      String variationValue) {
    this.banditKey = banditKey;
    this.flagKey = flagKey;
    this.allocationKey = allocationKey;
    this.variationKey = variationKey;
    this.variationValue = variationValue;
  }

  @Override
  public String toString() {
    return "BanditFlagVariation{" +
      "banditKey='" + banditKey + '\'' +
      ", flagKey='" + flagKey + '\'' +
      ", allocationKey='" + allocationKey + '\'' +
      ", variationKey='" + variationKey + '\'' +
      ", variationValue='" + variationValue + '\'' +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BanditFlagVariation that = (BanditFlagVariation) o;
    return Objects.equals(banditKey, that.banditKey)
            && Objects.equals(flagKey, that.flagKey)
            && Objects.equals(allocationKey, that.allocationKey)
            && Objects.equals(variationKey, that.variationKey)
            && Objects.equals(variationValue, that.variationValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(banditKey, flagKey, allocationKey, variationKey, variationValue);
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

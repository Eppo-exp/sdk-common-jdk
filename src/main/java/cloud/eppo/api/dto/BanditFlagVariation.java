package cloud.eppo.api.dto;

import java.util.Objects;

public interface BanditFlagVariation {
  String getBanditKey();

  String getFlagKey();

  String getAllocationKey();

  String getVariationKey();

  String getVariationValue();

  class Default implements BanditFlagVariation {
    private final String banditKey;
    private final String flagKey;
    private final String allocationKey;
    private final String variationKey;
    private final String variationValue;

    public Default(
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
      return "BanditFlagVariation{"
          + "banditKey='"
          + banditKey
          + '\''
          + ", flagKey='"
          + flagKey
          + '\''
          + ", allocationKey='"
          + allocationKey
          + '\''
          + ", variationKey='"
          + variationKey
          + '\''
          + ", variationValue='"
          + variationValue
          + '\''
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      BanditFlagVariation that = (BanditFlagVariation) o;
      return Objects.equals(banditKey, that.getBanditKey())
          && Objects.equals(flagKey, that.getFlagKey())
          && Objects.equals(allocationKey, that.getAllocationKey())
          && Objects.equals(variationKey, that.getVariationKey())
          && Objects.equals(variationValue, that.getVariationValue());
    }

    @Override
    public int hashCode() {
      return Objects.hash(banditKey, flagKey, allocationKey, variationKey, variationValue);
    }

    @Override
    public String getBanditKey() {
      return banditKey;
    }

    @Override
    public String getFlagKey() {
      return flagKey;
    }

    @Override
    public String getAllocationKey() {
      return allocationKey;
    }

    @Override
    public String getVariationKey() {
      return variationKey;
    }

    @Override
    public String getVariationValue() {
      return variationValue;
    }
  }
}

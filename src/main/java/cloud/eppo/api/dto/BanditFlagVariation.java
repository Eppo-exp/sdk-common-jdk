package cloud.eppo.api.dto;

import java.io.Serializable;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public interface BanditFlagVariation extends Serializable {
  @NotNull String getBanditKey();

  @NotNull String getFlagKey();

  @NotNull String getAllocationKey();

  @NotNull String getVariationKey();

  @NotNull String getVariationValue();

  class Default implements BanditFlagVariation {
    private static final long serialVersionUID = 1L;
    private final @NotNull String banditKey;
    private final @NotNull String flagKey;
    private final @NotNull String allocationKey;
    private final @NotNull String variationKey;
    private final @NotNull String variationValue;

    public Default(
        @NotNull String banditKey,
        @NotNull String flagKey,
        @NotNull String allocationKey,
        @NotNull String variationKey,
        @NotNull String variationValue) {
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
    @NotNull public String getBanditKey() {
      return banditKey;
    }

    @Override
    @NotNull public String getFlagKey() {
      return flagKey;
    }

    @Override
    @NotNull public String getAllocationKey() {
      return allocationKey;
    }

    @Override
    @NotNull public String getVariationKey() {
      return variationKey;
    }

    @Override
    @NotNull public String getVariationValue() {
      return variationValue;
    }
  }
}

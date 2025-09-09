package cloud.eppo.ufc.dto;

import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BanditFlagVariation {
  @NotNull private final String banditKey;
  @NotNull private final String flagKey;
  @NotNull private final String allocationKey;
  @NotNull private final String variationKey;
  @NotNull private final String variationValue;

  public BanditFlagVariation(
      @NotNull String banditKey,
      @NotNull String flagKey,
      @NotNull String allocationKey,
      @NotNull String variationKey,
      @NotNull String variationValue) {
    throwIfNull(banditKey, "banditKey must not be null");
    throwIfNull(flagKey, "flagKey must not be null");
    throwIfNull(allocationKey, "allocationKey must not be null");
    throwIfNull(variationKey, "variationKey must not be null");
    throwIfNull(variationValue, "variationValue must not be null");

    this.banditKey = banditKey;
    this.flagKey = flagKey;
    this.allocationKey = allocationKey;
    this.variationKey = variationKey;
    this.variationValue = variationValue;
  }

  @Override @NotNull
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
  public boolean equals(@Nullable Object o) {
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

  @NotNull
  public String getBanditKey() {
    return banditKey;
  }

  @NotNull
  public String getFlagKey() {
    return flagKey;
  }

  @NotNull
  public String getAllocationKey() {
    return allocationKey;
  }

  @NotNull
  public String getVariationKey() {
    return variationKey;
  }

  @NotNull
  public String getVariationValue() {
    return variationValue;
  }
}

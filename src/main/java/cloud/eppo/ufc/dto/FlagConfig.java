package cloud.eppo.ufc.dto;

import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FlagConfig {
  @NotNull private final String key;
  private final boolean enabled;
  private final int totalShards;
  @Nullable private final VariationType variationType;
  @NotNull private final Map<String, Variation> variations;
  @NotNull private final List<String> sortedVariationKeys;
  @NotNull private final List<Allocation> allocations;

  public FlagConfig(
      @NotNull String key,
      boolean enabled,
      int totalShards,
      @Nullable VariationType variationType,
      @NotNull Map<String, Variation> variations,
      @NotNull List<String> sortedVariationKeys,
      @NotNull List<Allocation> allocations) {
    throwIfNull(key, "key must not be null");
    throwIfNull(variations, "variations must not be null");
    throwIfNull(sortedVariationKeys, "sortedVariationKeys must not be null");
    throwIfNull(allocations, "allocations must not be null");

    this.key = key;
    this.enabled = enabled;
    this.totalShards = totalShards;
    this.variationType = variationType;
    this.variations = variations;
    this.sortedVariationKeys = sortedVariationKeys;
    this.allocations = allocations;
  }

  @Override @NotNull
  public String toString() {
    return "FlagConfig{" +
      "key='" + key + '\'' +
      ", enabled=" + enabled +
      ", totalShards=" + totalShards +
      ", variationType=" + variationType +
      ", variations=" + variations +
      ", sortedVariationKeys=" + sortedVariationKeys +
      ", allocations=" + allocations +
      '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    FlagConfig that = (FlagConfig) o;
    return enabled == that.enabled
            && totalShards == that.totalShards
            && Objects.equals(key, that.key)
            && variationType == that.variationType
            && Objects.equals(variations, that.variations)
            && Objects.equals(sortedVariationKeys, that.sortedVariationKeys)
            && Objects.equals(allocations, that.allocations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, enabled, totalShards, variationType, variations, sortedVariationKeys, allocations);
  }

  @NotNull
  public String getKey() {
    return this.key;
  }

  public int getTotalShards() {
    return totalShards;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Nullable
  public VariationType getVariationType() {
    return variationType;
  }

  @NotNull
  public Map<String, Variation> getVariations() {
    return variations;
  }

  @NotNull
  public List<String> getSortedVariationKeys() {
    return sortedVariationKeys;
  }

  @NotNull
  public List<Allocation> getAllocations() {
    return allocations;
  }
}

package cloud.eppo.ufc.dto;

import cloud.eppo.api.dto.Variation;
import cloud.eppo.api.dto.VariationType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FlagConfig {
  private final String key;
  private final boolean enabled;
  private final int totalShards;
  private final VariationType variationType;
  private final Map<String, Variation> variations;
  private final List<Allocation> allocations;

  public FlagConfig(
      String key,
      boolean enabled,
      int totalShards,
      VariationType variationType,
      Map<String, Variation> variations,
      List<Allocation> allocations) {
    this.key = key;
    this.enabled = enabled;
    this.totalShards = totalShards;
    this.variationType = variationType;
    this.variations = variations;
    this.allocations = allocations;
  }

  @Override
  public String toString() {
    return "FlagConfig{"
        + "key='"
        + key
        + '\''
        + ", enabled="
        + enabled
        + ", totalShards="
        + totalShards
        + ", variationType="
        + variationType
        + ", variations="
        + variations
        + ", allocations="
        + allocations
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    FlagConfig that = (FlagConfig) o;
    return enabled == that.enabled
        && totalShards == that.totalShards
        && Objects.equals(key, that.key)
        && variationType == that.variationType
        && Objects.equals(variations, that.variations)
        && Objects.equals(allocations, that.allocations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, enabled, totalShards, variationType, variations, allocations);
  }

  public String getKey() {
    return this.key;
  }

  public int getTotalShards() {
    return totalShards;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public VariationType getVariationType() {
    return variationType;
  }

  public Map<String, Variation> getVariations() {
    return variations;
  }

  public List<Allocation> getAllocations() {
    return allocations;
  }
}

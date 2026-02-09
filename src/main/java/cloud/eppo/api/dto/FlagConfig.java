package cloud.eppo.api.dto;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public interface FlagConfig {
  @NotNull String getKey();

  boolean isEnabled();

  int getTotalShards();

  @NotNull VariationType getVariationType();

  @NotNull Map<String, Variation> getVariations();

  @NotNull List<Allocation> getAllocations();

  class Default implements FlagConfig {
    private final String key;
    private final boolean enabled;
    private final int totalShards;
    private final VariationType variationType;
    private final Map<String, Variation> variations;
    private final List<Allocation> allocations;

    public Default(
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
      return enabled == that.isEnabled()
          && totalShards == that.getTotalShards()
          && Objects.equals(key, that.getKey())
          && variationType == that.getVariationType()
          && Objects.equals(variations, that.getVariations())
          && Objects.equals(allocations, that.getAllocations());
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, enabled, totalShards, variationType, variations, allocations);
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public int getTotalShards() {
      return totalShards;
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public VariationType getVariationType() {
      return variationType;
    }

    @Override
    public Map<String, Variation> getVariations() {
      return variations;
    }

    @Override
    public List<Allocation> getAllocations() {
      return allocations;
    }
  }
}

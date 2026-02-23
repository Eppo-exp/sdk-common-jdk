package cloud.eppo.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FlagConfig extends Serializable {
  @NotNull String getKey();

  boolean isEnabled();

  int getTotalShards();

  @NotNull VariationType getVariationType();

  @NotNull Map<String, Variation> getVariations();

  @NotNull List<Allocation> getAllocations();

  class Default implements FlagConfig {
    private static final long serialVersionUID = 1L;
    private final @NotNull String key;
    private final boolean enabled;
    private final int totalShards;
    private final @NotNull VariationType variationType;
    private final @NotNull Map<String, Variation> variations;
    private final @NotNull List<Allocation> allocations;

    public Default(
        @NotNull String key,
        boolean enabled,
        int totalShards,
        @Nullable VariationType variationType,
        @Nullable Map<String, Variation> variations,
        @Nullable List<Allocation> allocations) {
      this.key = key;
      this.enabled = enabled;
      this.totalShards = totalShards;
      this.variationType = variationType == null ? VariationType.STRING : variationType;
      this.variations =
          variations == null
              ? Collections.emptyMap()
              : Collections.unmodifiableMap(new HashMap<>(variations));
      this.allocations =
          allocations == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(allocations));
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
    @NotNull public String getKey() {
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
    @NotNull public VariationType getVariationType() {
      return variationType;
    }

    @Override
    @NotNull public Map<String, Variation> getVariations() {
      return variations;
    }

    @Override
    @NotNull public List<Allocation> getAllocations() {
      return allocations;
    }
  }
}

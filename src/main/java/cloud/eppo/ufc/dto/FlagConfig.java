package cloud.eppo.ufc.dto;

import java.util.List;
import java.util.Map;

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

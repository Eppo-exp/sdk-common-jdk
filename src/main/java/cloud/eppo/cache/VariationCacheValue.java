package cloud.eppo.cache;

import java.util.Objects;

public class VariationCacheValue implements AssignmentCacheValue {
  private final String allocationKey;
  private final String variationKey;

  public VariationCacheValue(String allocationKey, String variationKey) {
    this.allocationKey = allocationKey;
    this.variationKey = variationKey;
  }

  @Override
  public String getValueIdentifier() {
    return allocationKey + "-" + variationKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VariationCacheValue that = (VariationCacheValue) o;
    return Objects.equals(allocationKey, that.allocationKey)
        && Objects.equals(variationKey, that.variationKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allocationKey, variationKey);
  }
}

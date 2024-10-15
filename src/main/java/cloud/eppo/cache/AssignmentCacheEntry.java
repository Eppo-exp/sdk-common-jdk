package cloud.eppo.cache;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class AssignmentCacheEntry {
  private final AssignmentCacheKey key;
  private final AssignmentCacheValue value;

  public AssignmentCacheEntry(
      @NotNull AssignmentCacheKey key, @NotNull AssignmentCacheValue value) {
    this.key = key;
    this.value = value;
  }

  @NotNull public AssignmentCacheKey getKey() {
    return key;
  }

  @NotNull public String getKeyString() {
    return key.toString();
  }

  @NotNull public String getValueKeyString() {
    return value.getValueKey();
  }

  @NotNull public AssignmentCacheValue getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AssignmentCacheEntry that = (AssignmentCacheEntry) o;
    return Objects.equals(key, that.key)
        && Objects.equals(value.getValueKey(), that.value.getValueKey());
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }
}

package cloud.eppo.cache;

import static cloud.eppo.Utils.throwIfNull;

import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.BanditAssignment;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssignmentCacheEntry {
  @NotNull private final AssignmentCacheKey key;
  @NotNull private final AssignmentCacheValue value;

  public AssignmentCacheEntry(
      @NotNull AssignmentCacheKey key, @NotNull AssignmentCacheValue value) {
    throwIfNull(key, "key must not be null");
    throwIfNull(value, "value must not be null");

    this.key = key;
    this.value = value;
  }

  @NotNull
  public static AssignmentCacheEntry fromVariationAssignment(@NotNull Assignment assignment) {
    return new AssignmentCacheEntry(
        new AssignmentCacheKey(assignment.getSubject(), assignment.getFeatureFlag()),
        new VariationCacheValue(assignment.getAllocation(), assignment.getVariation()));
  }

  @NotNull
  public static AssignmentCacheEntry fromBanditAssignment(@NotNull BanditAssignment assignment) {
    return new AssignmentCacheEntry(
        new AssignmentCacheKey(assignment.getSubject(), assignment.getFeatureFlag()),
        new BanditCacheValue(assignment.getBandit(), assignment.getAction()));
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    AssignmentCacheEntry that = (AssignmentCacheEntry) o;
    return Objects.equals(key, that.key)
      && Objects.equals(value.getValueIdentifier(), that.value.getValueIdentifier());
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override @NotNull
  public String toString() {
    return "AssignmentCacheEntry{" +
      "key=" + key +
      ", value=" + value +
      '}';
  }

  @NotNull public AssignmentCacheKey getKey() {
    return key;
  }

  @NotNull public String getKeyString() {
    return key.toString();
  }

  @NotNull public String getValueKeyString() {
    return value.getValueIdentifier();
  }

  @NotNull public AssignmentCacheValue getValue() {
    return value;
  }

}

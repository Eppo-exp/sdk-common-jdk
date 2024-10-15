package cloud.eppo.cache;

import cloud.eppo.logging.Assignment;
import cloud.eppo.logging.BanditAssignment;
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

  public static AssignmentCacheEntry fromVariationAssignment(Assignment assignment) {
    return new AssignmentCacheEntry(
        new AssignmentCacheKey(assignment.getSubject(), assignment.getFeatureFlag()),
        new VariationCacheValue(assignment.getAllocation(), assignment.getVariation()));
  }

  public static AssignmentCacheEntry fromBanditAssignment(BanditAssignment assignment) {
    return new AssignmentCacheEntry(
        new AssignmentCacheKey(assignment.getSubject(), assignment.getFeatureFlag()),
        new BanditCacheValue(assignment.getBandit(), assignment.getAction()));
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

package cloud.eppo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import cloud.eppo.ufc.dto.Variation;

/**
 * Holds an allocation key and a variation because these two values
 * either both need to be null or both need to be not-null in a
 * FlagEvaluationResult. This makes nullability easier to enforce.
 */
public class FlagEvaluationAllocationKeyAndVariation {
  @NotNull
  final String allocationKey;
  @NotNull final Variation variation;

  public FlagEvaluationAllocationKeyAndVariation(@NotNull String allocationKey, @NotNull Variation variation) {
    this.allocationKey = allocationKey;
    this.variation = variation;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    FlagEvaluationAllocationKeyAndVariation that = (FlagEvaluationAllocationKeyAndVariation) o;
    return Objects.equals(allocationKey, that.allocationKey) && Objects.equals(variation, that.variation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allocationKey, variation);
  }

  @Override
  public String toString() {
    return "AllocationKeyAndVariation{" +
      "allocationKey='" + allocationKey + '\'' +
      ", variation=" + variation +
      '}';
  }

  @NotNull
  public String getAllocationKey() {
    return allocationKey;
  }

  @NotNull
  public Variation getVariation() {
    return variation;
  }
}

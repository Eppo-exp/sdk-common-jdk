package cloud.eppo.ufc.dto;

import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Split {
  @NotNull private final String variationKey;
  @NotNull private final Set<Shard> shards;
  @NotNull private final Map<String, String> extraLogging;

  public Split(
      @NotNull String variationKey,
      @NotNull Set<Shard> shards,
      @NotNull Map<String, String> extraLogging) {
    throwIfNull(variationKey, "variationKey must not be null");
    throwIfNull(shards, "shards must not be null");
    throwIfNull(extraLogging, "extraLogging must not be null");

    this.variationKey = variationKey;
    this.shards = shards;
    this.extraLogging = extraLogging;
  }

  @Override @NotNull
  public String toString() {
    return "Split{" +
      "variationKey='" + variationKey + '\'' +
      ", shards=" + shards +
      ", extraLogging=" + extraLogging +
      '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Split split = (Split) o;
    return Objects.equals(variationKey, split.variationKey)
            && Objects.equals(shards, split.shards)
            && Objects.equals(extraLogging, split.extraLogging);
  }

  @Override
  public int hashCode() {
    return Objects.hash(variationKey, shards, extraLogging);
  }

  @NotNull
  public String getVariationKey() {
    return variationKey;
  }

  @NotNull
  public Set<Shard> getShards() {
    return shards;
  }

  @NotNull
  public Map<String, String> getExtraLogging() {
    return extraLogging;
  }
}

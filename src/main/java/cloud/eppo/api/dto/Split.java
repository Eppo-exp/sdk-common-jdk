package cloud.eppo.api.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Split extends Serializable {
  @NotNull String getVariationKey();

  @NotNull Set<Shard> getShards();

  @NotNull Map<String, String> getExtraLogging();

  class Default implements Split {
    private static final long serialVersionUID = 1L;
    private final @NotNull String variationKey;
    private final @NotNull Set<Shard> shards;
    private final @NotNull Map<String, String> extraLogging;

    public Default(
        @NotNull String variationKey,
        @Nullable Set<Shard> shards,
        @Nullable Map<String, String> extraLogging) {
      this.variationKey = variationKey;
      this.shards = shards == null
          ? Collections.emptySet()
          : Collections.unmodifiableSet(new HashSet<>(shards));
      this.extraLogging = extraLogging == null
          ? Collections.emptyMap()
          : Collections.unmodifiableMap(new HashMap<>(extraLogging));
    }

    @Override
    public String toString() {
      return "Split{"
          + "variationKey='"
          + variationKey
          + '\''
          + ", shards="
          + shards
          + ", extraLogging="
          + extraLogging
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      Split split = (Split) o;
      return Objects.equals(variationKey, split.getVariationKey())
          && Objects.equals(shards, split.getShards())
          && Objects.equals(extraLogging, split.getExtraLogging());
    }

    @Override
    public int hashCode() {
      return Objects.hash(variationKey, shards, extraLogging);
    }

    @Override
    @NotNull
    public String getVariationKey() {
      return variationKey;
    }

    @Override
    @NotNull
    public Set<Shard> getShards() {
      return shards;
    }

    @Override
    @NotNull
    public Map<String, String> getExtraLogging() {
      return extraLogging;
    }
  }
}

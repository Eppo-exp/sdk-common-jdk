package cloud.eppo.api.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public interface Split extends Serializable {
  @NotNull String getVariationKey();

  @NotNull Set<Shard> getShards();

  @NotNull Map<String, String> getExtraLogging();

  class Default implements Split {
    private static final long serialVersionUID = 1L;
    private final String variationKey;
    private final Set<Shard> shards;
    private final Map<String, String> extraLogging;

    public Default(String variationKey, Set<Shard> shards, Map<String, String> extraLogging) {
      this.variationKey = variationKey;
      this.shards = shards;
      this.extraLogging = extraLogging;
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
    public String getVariationKey() {
      return variationKey;
    }

    @Override
    public Set<Shard> getShards() {
      return shards;
    }

    @Override
    public Map<String, String> getExtraLogging() {
      return extraLogging;
    }
  }
}

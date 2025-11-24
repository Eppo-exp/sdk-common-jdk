package cloud.eppo.ufc.dto;

import cloud.eppo.api.ISplit;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Split implements ISplit {
  private final String variationKey;
  private final Set<Shard> shards;
  private final Map<String, String> extraLogging;

  public Split(String variationKey, Set<Shard> shards, Map<String, String> extraLogging) {
    this.variationKey = variationKey;
    this.shards = shards;
    this.extraLogging = extraLogging;
  }

  @Override
  public String toString() {
    return "Split{" +
      "variationKey='" + variationKey + '\'' +
      ", shards=" + shards +
      ", extraLogging=" + extraLogging +
      '}';
  }

  @Override
  public boolean equals(Object o) {
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

  public String getVariationKey() {
    return variationKey;
  }

  public Set<Shard> getShards() {
    return shards;
  }

  public Map<String, String> getExtraLogging() {
    return extraLogging;
  }
}

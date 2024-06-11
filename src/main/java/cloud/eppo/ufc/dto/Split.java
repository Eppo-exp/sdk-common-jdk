package cloud.eppo.ufc.dto;

import java.util.Map;
import java.util.Set;

public class Split {
  private final String variationKey;
  private final Set<Shard> shards;
  private final Map<String, String> extraLogging;

  public Split(String variationKey, Set<Shard> shards, Map<String, String> extraLogging) {
    this.variationKey = variationKey;
    this.shards = shards;
    this.extraLogging = extraLogging;
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

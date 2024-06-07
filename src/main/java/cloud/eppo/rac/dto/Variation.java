package cloud.eppo.rac.dto;

import cloud.eppo.model.ShardRange;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Experiment's Variation Class */
public class Variation {
  private final String name;
  private EppoValue typedValue;
  private final ShardRange shardRange;
  private final AlgorithmType algorithmType;

  @JsonCreator
  public Variation(
      @JsonProperty("name") String name,
      @JsonProperty("typedValue") EppoValue typedValue,
      @JsonProperty("shardRange") ShardRange shardRange,
      @JsonProperty("algorithmType") AlgorithmType algorithmType) {
    this.name = name;
    this.typedValue = typedValue;
    this.shardRange = shardRange;
    this.algorithmType = algorithmType;
  }

  public String getName() {
    return name;
  }

  public EppoValue getTypedValue() {
    return typedValue;
  }

  public void setTypedValue(EppoValue typedValue) {
    this.typedValue = typedValue;
  }

  public ShardRange getShardRange() {
    return shardRange;
  }

  public AlgorithmType getAlgorithmType() {
    return algorithmType;
  }
}

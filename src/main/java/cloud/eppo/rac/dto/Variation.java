package cloud.eppo.rac.dto;

/** Experiment's Variation Class */
public class Variation {
  private final String name;
  private EppoValue typedValue;
  private final ShardRange shardRange;
  private final AlgorithmType algorithmType;

  public Variation(
      String name, EppoValue typedValue, ShardRange shardRange, AlgorithmType algorithmType) {
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

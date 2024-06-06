package cloud.eppo.rac.dto;

import lombok.Data;

/** Experiment's Variation Class */
@Data
public class Variation {
  private String name;
  private EppoValue typedValue;
  private ShardRange shardRange;
  private AlgorithmType algorithmType;
}

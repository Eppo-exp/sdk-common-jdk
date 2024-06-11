package cloud.eppo.ufc.dto;

import cloud.eppo.model.ShardRange;
import java.util.Set;

public class Shard {
  private final String salt;
  private final Set<ShardRange> ranges;

  public Shard(String salt, Set<ShardRange> ranges) {
    this.salt = salt;
    this.ranges = ranges;
  }

  public String getSalt() {
    return salt;
  }

  public Set<ShardRange> getRanges() {
    return ranges;
  }
}

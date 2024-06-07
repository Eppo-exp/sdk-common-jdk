package cloud.eppo.ufc.dto;

import cloud.eppo.model.ShardRange;
import java.util.Set;

public class Shard {
  private String salt;
  private Set<ShardRange> ranges;

  public String getSalt() {
    return salt;
  }

  public void setSalt(String salt) {
    this.salt = salt;
  }

  public Set<ShardRange> getRanges() {
    return ranges;
  }

  public void setRanges(Set<ShardRange> ranges) {
    this.ranges = ranges;
  }
}

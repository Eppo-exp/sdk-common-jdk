package cloud.eppo.ufc.dto;

import cloud.eppo.model.ShardRange;

import java.util.Objects;
import java.util.Set;

public class Shard {
  private final String salt;
  private final Set<ShardRange> ranges;

  public Shard(String salt, Set<ShardRange> ranges) {
    this.salt = salt;
    this.ranges = ranges;
  }

  @Override
  public String toString() {
    return "Shard{" +
      "salt='" + salt + '\'' +
      ", ranges=" + ranges +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Shard shard = (Shard) o;
    return Objects.equals(salt, shard.salt)
            && Objects.equals(ranges, shard.ranges);
  }

  @Override
  public int hashCode() {
    return Objects.hash(salt, ranges);
  }

  public String getSalt() {
    return salt;
  }

  public Set<ShardRange> getRanges() {
    return ranges;
  }
}

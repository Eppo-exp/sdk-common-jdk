package cloud.eppo.api.dto;

import cloud.eppo.model.ShardRange;
import java.util.Objects;
import java.util.Set;

public interface Shard {
  String getSalt();

  Set<ShardRange> getRanges();

  class Default implements Shard {
    private final String salt;
    private final Set<ShardRange> ranges;

    public Default(String salt, Set<ShardRange> ranges) {
      this.salt = salt;
      this.ranges = ranges;
    }

    @Override
    public String toString() {
      return "Shard{" + "salt='" + salt + '\'' + ", ranges=" + ranges + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      Shard shard = (Shard) o;
      return Objects.equals(salt, shard.getSalt()) && Objects.equals(ranges, shard.getRanges());
    }

    @Override
    public int hashCode() {
      return Objects.hash(salt, ranges);
    }

    @Override
    public String getSalt() {
      return salt;
    }

    @Override
    public Set<ShardRange> getRanges() {
      return ranges;
    }
  }
}

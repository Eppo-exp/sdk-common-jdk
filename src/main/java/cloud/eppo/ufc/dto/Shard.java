package cloud.eppo.ufc.dto;

import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import cloud.eppo.model.ShardRange;

import java.util.Objects;
import java.util.Set;

public class Shard {
  @NotNull private final String salt;
  @NotNull private final Set<ShardRange> ranges;

  public Shard(@NotNull String salt, @NotNull Set<ShardRange> ranges) {
    throwIfNull(salt, "salt must not be null");
    throwIfNull(ranges, "ranges must not be null");

    this.salt = salt;
    this.ranges = ranges;
  }

  @Override @NotNull
  public String toString() {
    return "Shard{" +
      "salt='" + salt + '\'' +
      ", ranges=" + ranges +
      '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Shard shard = (Shard) o;
    return Objects.equals(salt, shard.salt)
            && Objects.equals(ranges, shard.ranges);
  }

  @Override
  public int hashCode() {
    return Objects.hash(salt, ranges);
  }

  @NotNull
  public String getSalt() {
    return salt;
  }

  @NotNull
  public Set<ShardRange> getRanges() {
    return ranges;
  }
}

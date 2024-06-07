package cloud.eppo;

import cloud.eppo.model.ShardRange;

public final class ShardUtils {
  /** Returns whether the given shard is within the range. */
  public static boolean isShardInRange(int shard, ShardRange range) {
    return shard >= range.getStart() && shard < range.getEnd();
  }

  public static int getShard(String input, int maxShardValue) {
    StringBuilder hashText = new StringBuilder(Utils.getMD5Hex(input));
    while (hashText.length() < 32) {
      hashText.insert(0, "0");
    }
    return (int) (Long.parseLong(hashText.substring(0, 8), 16) % maxShardValue);
  }
}

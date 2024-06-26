package cloud.eppo.rac.dto;

import cloud.eppo.ShardUtils;
import cloud.eppo.model.ShardRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShardUtilsTest {
  ShardRange createShardRange(int start, int end) {
    return new ShardRange(start, end);
  }

  @DisplayName("Test Shard.isShardInRange() positive case")
  @Test
  void testIsShardInRangePositiveCase() {
    ShardRange range = createShardRange(10, 20);
    Assertions.assertTrue(ShardUtils.isShardInRange(15, range));
  }

  @DisplayName("Test Shard.isShardInRange() negative case")
  @Test
  void testIsShardInRangeNegativeCase() {
    ShardRange range = createShardRange(10, 20);
    Assertions.assertTrue(ShardUtils.isShardInRange(15, range));
  }

  @DisplayName("Test Shard.getShard()")
  @Test
  void testGetShard() {
    final int MAX_SHARD_VALUE = 200;
    int shardValue = ShardUtils.getShard("test-user", MAX_SHARD_VALUE);
    Assertions.assertTrue(shardValue >= 0 & shardValue <= MAX_SHARD_VALUE);
  }
}

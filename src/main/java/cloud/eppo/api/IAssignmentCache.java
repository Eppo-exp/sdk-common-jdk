package cloud.eppo.api;

import cloud.eppo.cache.AssignmentCacheEntry;
import java.util.concurrent.CompletableFuture;

/**
 * A cache capable of storing the key components of assignments (both variation and bandit) to
 * determine both presence and uniqueness of the cached value.
 */
public interface IAssignmentCache {
  CompletableFuture<Void> set(AssignmentCacheEntry entry);

  /**
   * Determines whether the entry is present. Implementations must first check for presence by using
   * the `{@link AssignmentCacheEntry}.getKey()` method and then whether the cached value matches by
   * comparing the `getValueKeyString()` method results.
   */
  CompletableFuture<Boolean> hasEntry(AssignmentCacheEntry entry);
}

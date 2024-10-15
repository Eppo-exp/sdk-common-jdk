package cloud.eppo.cache;

import java.util.concurrent.CompletableFuture;

public interface AssignmentCache {
  CompletableFuture<Void> set(AssignmentCacheEntry entry);

  /**
   * Checks for both the existence of an entry using both the entry's key
   * (AssignmentCacheEntry.getKey()) and the identifier for the value
   * (AssignmentCacheEntry.getValue().getValueKey());
   */
  CompletableFuture<Boolean> hasEntry(AssignmentCacheEntry entry);
}

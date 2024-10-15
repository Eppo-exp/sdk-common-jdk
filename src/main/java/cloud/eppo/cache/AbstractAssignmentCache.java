package cloud.eppo.cache;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractAssignmentCache implements AssignmentCache {
  protected final Map<String, String> delegate;
  // key -> variation value hash
  protected AbstractAssignmentCache(final Map<String, String> delegate) {
    this.delegate = delegate;
  }

  /** Returns whether the provided {@link AssignmentCacheEntry} is present in the cache. */
  public CompletableFuture<Boolean> hasEntry(AssignmentCacheEntry entry) {
    return get(entry.getKey())
        .thenApply(value -> value != null && value.equals(entry.getValueKeyString()));
  }

  private CompletableFuture<String> get(AssignmentCacheKey key) {
    return CompletableFuture.supplyAsync(() -> this.delegate.get(key.toString()));
  }

  /**
   * Stores the provided {@link AssignmentCacheEntry} in the cache. If the key already exists, it
   * will be overwritten.
   */
  public CompletableFuture<Void> set(AssignmentCacheEntry entry) {
    return CompletableFuture.runAsync(
        () -> {
          this.delegate.put(entry.getKeyString(), entry.getValueKeyString());
        });
  }
  //
  //  /**
  //   * Returns an array with all {@link AssignmentCacheEntry} entries in the cache as an array of
  //   * {@link String}s.
  //   */
  //  public IterableIterator<[String, String]> entries() {
  //    return this.delegate.entries();
  //  }

  //  private String assignmentCacheValueToString(AssignmentCacheValue value) {
  //    return value.get("allocationKey") + ";" + value.get("variationKey");
  //  }
}

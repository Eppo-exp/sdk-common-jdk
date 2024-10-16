package cloud.eppo.api;

import cloud.eppo.cache.AssignmentCacheEntry;
import cloud.eppo.cache.AssignmentCacheKey;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * {@link IAssignmentCache} implementation which takes a map to use as the underlying storage
 * mechanism.
 */
public abstract class AbstractAssignmentCache implements IAssignmentCache {
  protected final Map<String, String> delegate;

  protected AbstractAssignmentCache(final Map<String, String> delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableFuture<Boolean> hasEntry(AssignmentCacheEntry entry) {
    return get(entry.getKey())
        .thenApply(value -> value != null && value.equals(entry.getValueKeyString()));
  }

  private CompletableFuture<String> get(AssignmentCacheKey key) {
    return CompletableFuture.supplyAsync(() -> this.delegate.get(key.toString()));
  }

  @Override
  public CompletableFuture<Void> put(AssignmentCacheEntry entry) {
    return CompletableFuture.runAsync(
        () -> {
          this.delegate.put(entry.getKeyString(), entry.getValueKeyString());
        });
  }
}

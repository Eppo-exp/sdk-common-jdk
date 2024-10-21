package cloud.eppo.api;

import cloud.eppo.cache.AssignmentCacheEntry;
import cloud.eppo.cache.AssignmentCacheKey;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link IAssignmentCache} implementation which takes a map to use as the underlying storage
 * mechanism.
 */
public abstract class AbstractAssignmentCache implements IAssignmentCache {

  /** Minimal "map" implementation required to store cached assignment data. */
  public interface CacheDelegate {
    void put(String cacheKey, @NotNull String serializedEntry);

    @Nullable String get(String cacheKey);
  }

  protected final CacheDelegate delegate;

  protected AbstractAssignmentCache(final CacheDelegate delegate) {
    this.delegate = delegate;
  }

  protected AbstractAssignmentCache(final Map<String, String> delegate) {
    this(
        new CacheDelegate() {

          @Override
          public void put(String cacheKey, @NotNull String serializedEntry) {
            delegate.put(cacheKey, serializedEntry);
          }

          @Nullable @Override
          public String get(String cacheKey) {
            return delegate.get(cacheKey);
          }
        });
  }

  @Override
  public boolean hasEntry(AssignmentCacheEntry entry) {
    String serializedEntry = get(entry.getKey());
    return serializedEntry != null && serializedEntry.equals(entry.getValueKeyString());
  }

  private String get(AssignmentCacheKey key) {
    return delegate.get(key.toString());
  }

  @Override
  public void put(AssignmentCacheEntry entry) {
    delegate.put(entry.getKeyString(), entry.getValueKeyString());
  }
}

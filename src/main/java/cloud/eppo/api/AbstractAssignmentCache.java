package cloud.eppo.api;

import cloud.eppo.cache.AssignmentCacheEntry;
import cloud.eppo.cache.AssignmentCacheKey;
import java.util.Map;

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
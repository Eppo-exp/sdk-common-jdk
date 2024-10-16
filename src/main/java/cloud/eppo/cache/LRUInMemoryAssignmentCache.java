package cloud.eppo.cache;

import cloud.eppo.api.AbstractAssignmentCache;
import java.util.Collections;
import org.apache.commons.collections4.map.LRUMap;

/**
 * A cache that uses the LRU algorithm to evict the least recently used items.
 *
 * <p>The primary use case is for server-side SDKs, where the cache is shared across multiple users.
 */
public class LRUInMemoryAssignmentCache extends AbstractAssignmentCache {
  public LRUInMemoryAssignmentCache(int maxSize) {

    // Synchronized wrapper for thread safety
    super(Collections.synchronizedMap(new LRUMap<>(maxSize)));
  }
}

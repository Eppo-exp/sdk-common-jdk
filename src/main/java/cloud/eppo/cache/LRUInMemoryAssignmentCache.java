package cloud.eppo.cache;

import org.apache.commons.collections4.map.LRUMap;

import java.util.Collections;

/**
 * spot A cache that uses the LRU algorithm to evict the least recently used items.
 *
 * <p>It is used to limit the size of the cache.
 *
 * <p>The primary use case is for server-side SDKs, where the cache is shared across multiple users.
 * In this case, the cache size should be set to the maximum number of users that can be active at
 * the same time.
 */
public class LRUInMemoryAssignmentCache extends AbstractAssignmentCache {
  public LRUInMemoryAssignmentCache(int maxSize) {

    // Synchronized wrapper for thread safety
    super(Collections.synchronizedMap(new LRUMap<String, String>(0, maxSize)));
  }
}

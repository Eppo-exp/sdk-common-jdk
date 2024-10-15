package cloud.eppo.cache;

import java.util.HashMap;

/**
 * A cache that never expires.
 *
 * <p>The primary use case is for client-side SDKs, where the cache is only used for a single user.
 */
public class NonExpiringInMemoryAssignmentCache extends AbstractAssignmentCache {
  public NonExpiringInMemoryAssignmentCache() {
    super(new HashMap<>());
  }
}

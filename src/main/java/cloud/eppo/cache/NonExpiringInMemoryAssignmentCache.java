package cloud.eppo.cache;

import cloud.eppo.api.AbstractAssignmentCache;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.ApiStatus;

/**
 * A cache that never expires.
 *
 * <p>The primary use case is for client-side SDKs, where the cache is only used for a single user
 * and won't grow out of control.
 */
@ApiStatus.Internal
public class NonExpiringInMemoryAssignmentCache extends AbstractAssignmentCache {
  public NonExpiringInMemoryAssignmentCache() {
    super(new ConcurrentHashMap<>());
  }
}

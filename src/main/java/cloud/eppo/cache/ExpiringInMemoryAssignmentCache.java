package cloud.eppo.cache;

import cloud.eppo.api.AbstractAssignmentCache;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.map.PassiveExpiringMap;

public class ExpiringInMemoryAssignmentCache extends AbstractAssignmentCache {
  public ExpiringInMemoryAssignmentCache(int cacheTimeoutInSeconds) {
    super(
        Collections.synchronizedMap(
            new PassiveExpiringMap<>(cacheTimeoutInSeconds, TimeUnit.SECONDS)));
  }

  public ExpiringInMemoryAssignmentCache(
      Map<String, String> delegate, int cacheTimeout, TimeUnit timeUnit) {
    super(Collections.synchronizedMap(new PassiveExpiringMap<>(cacheTimeout, timeUnit, delegate)));
  }
}

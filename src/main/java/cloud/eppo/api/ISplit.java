package cloud.eppo.api;

import java.util.Map;
import java.util.Set;

/**
 * Interface for Split allowing downstream SDKs to provide custom implementations.
 */
public interface ISplit {
  String getVariationKey();

  Set<? extends IShard> getShards();

  Map<String, String> getExtraLogging();
}

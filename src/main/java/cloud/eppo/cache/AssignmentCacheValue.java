package cloud.eppo.cache;

import java.io.Serializable;

public interface AssignmentCacheValue extends Serializable {
  String getValueKey();
}

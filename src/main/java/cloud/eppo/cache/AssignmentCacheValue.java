package cloud.eppo.cache;

import java.io.Serializable;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface AssignmentCacheValue extends Serializable {
  String getValueIdentifier();
}

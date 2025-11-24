package cloud.eppo.api;

import java.util.Set;

/** Interface for TargetingRule allowing downstream SDKs to provide custom implementations. */
public interface ITargetingRule {
  Set<? extends ITargetingCondition> getConditions();
}

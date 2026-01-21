package cloud.eppo.api;

import java.util.Date;
import java.util.List;
import java.util.Set;

/** Interface for Allocation allowing downstream SDKs to provide custom implementations. */
public interface IAllocation {
  String getKey();

  Set<? extends ITargetingRule> getRules();

  Date getStartAt();

  Date getEndAt();

  List<? extends ISplit> getSplits();

  boolean doLog();
}

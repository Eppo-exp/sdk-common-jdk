package cloud.eppo.api;

import java.util.Set;

/** Interface for Shard allowing downstream SDKs to provide custom implementations. */
public interface IShard {
  String getSalt();

  Set<? extends IShardRange> getRanges();
}

package cloud.eppo.api;

/**
 * Interface for ShardRange allowing downstream SDKs to provide custom implementations.
 */
public interface IShardRange {
  int getStart();

  int getEnd();
}

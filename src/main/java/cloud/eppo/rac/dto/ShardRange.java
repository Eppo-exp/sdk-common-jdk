package cloud.eppo.rac.dto;

/** Shard Range Class */
public class ShardRange {
  public int start;
  public int end;

  @Override
  public String toString() {
    return "[start: " + start + "| end: " + end + "]";
  }
}

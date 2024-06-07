package cloud.eppo.rac.dto;

/** Shard Range Class */
public class ShardRange {
  private final int start;
  private int end;

  public ShardRange(int start, int end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public String toString() {
    return "[start: " + start + "| end: " + end + "]";
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }

  public void setEnd(int end) {
    this.end = end;
  }
}

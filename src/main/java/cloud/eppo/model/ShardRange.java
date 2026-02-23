package cloud.eppo.model;

import java.io.Serializable;
import java.util.Objects;

/** Shard Range Class */
public class ShardRange implements Serializable {
  private static final long serialVersionUID = 1L;
  private final int start;
  private int end;

  public ShardRange(int start, int end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ShardRange that = (ShardRange) o;
    return start == that.start && end == that.end;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
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

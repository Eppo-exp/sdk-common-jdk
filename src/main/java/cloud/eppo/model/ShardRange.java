package cloud.eppo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Shard Range Class */
public class ShardRange {
  private final int start;
  private int end;

  @JsonCreator
  public ShardRange(@JsonProperty("start") int start, @JsonProperty("end") int end) {
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

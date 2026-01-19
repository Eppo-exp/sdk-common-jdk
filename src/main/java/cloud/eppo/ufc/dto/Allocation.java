package cloud.eppo.ufc.dto;

import cloud.eppo.api.IAllocation;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Allocation implements IAllocation {
  private String key;
  private Set<TargetingRule> rules;
  private Date startAt;
  private Date endAt;
  private List<Split> splits;
  private boolean doLog;

  public Allocation(
      String key,
      Set<TargetingRule> rules,
      Date startAt,
      Date endAt,
      List<Split> splits,
      boolean doLog) {
    this.key = key;
    this.rules = rules;
    this.startAt = startAt;
    this.endAt = endAt;
    this.splits = splits;
    this.doLog = doLog;
  }

  @Override
  public String toString() {
    return "Allocation{" +
      "key='" + key + '\'' +
      ", rules=" + rules +
      ", startAt=" + startAt +
      ", endAt=" + endAt +
      ", splits=" + splits +
      ", doLog=" + doLog +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Allocation that = (Allocation) o;
    return doLog == that.doLog
            && Objects.equals(key, that.key)
            && Objects.equals(rules, that.rules)
            && Objects.equals(startAt, that.startAt)
            && Objects.equals(endAt, that.endAt)
            && Objects.equals(splits, that.splits);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, rules, startAt, endAt, splits, doLog);
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Set<TargetingRule> getRules() {
    return rules;
  }

  public void setRules(Set<TargetingRule> rules) {
    this.rules = rules;
  }

  public Date getStartAt() {
    return startAt;
  }

  public void setStartAt(Date startAt) {
    this.startAt = startAt;
  }

  public Date getEndAt() {
    return endAt;
  }

  public void setEndAt(Date endAt) {
    this.endAt = endAt;
  }

  public List<Split> getSplits() {
    return splits;
  }

  public void setSplits(List<Split> splits) {
    this.splits = splits;
  }

  public boolean doLog() {
    return doLog;
  }

  public void setDoLog(boolean doLog) {
    this.doLog = doLog;
  }
}

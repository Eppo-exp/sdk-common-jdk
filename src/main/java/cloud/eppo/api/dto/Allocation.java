package cloud.eppo.api.dto;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface Allocation {
  String getKey();

  void setKey(String key);

  Set<TargetingRule> getRules();

  void setRules(Set<TargetingRule> rules);

  Date getStartAt();

  void setStartAt(Date startAt);

  Date getEndAt();

  void setEndAt(Date endAt);

  List<Split> getSplits();

  void setSplits(List<Split> splits);

  boolean doLog();

  void setDoLog(boolean doLog);

  class Default implements Allocation {
    private String key;
    private Set<TargetingRule> rules;
    private Date startAt;
    private Date endAt;
    private List<Split> splits;
    private boolean doLog;

    public Default(
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
      return "Allocation{"
          + "key='"
          + key
          + '\''
          + ", rules="
          + rules
          + ", startAt="
          + startAt
          + ", endAt="
          + endAt
          + ", splits="
          + splits
          + ", doLog="
          + doLog
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      Allocation that = (Allocation) o;
      return doLog == that.doLog()
          && Objects.equals(key, that.getKey())
          && Objects.equals(rules, that.getRules())
          && Objects.equals(startAt, that.getStartAt())
          && Objects.equals(endAt, that.getEndAt())
          && Objects.equals(splits, that.getSplits());
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, rules, startAt, endAt, splits, doLog);
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public void setKey(String key) {
      this.key = key;
    }

    @Override
    public Set<TargetingRule> getRules() {
      return rules;
    }

    @Override
    public void setRules(Set<TargetingRule> rules) {
      this.rules = rules;
    }

    @Override
    public Date getStartAt() {
      return startAt;
    }

    @Override
    public void setStartAt(Date startAt) {
      this.startAt = startAt;
    }

    @Override
    public Date getEndAt() {
      return endAt;
    }

    @Override
    public void setEndAt(Date endAt) {
      this.endAt = endAt;
    }

    @Override
    public List<Split> getSplits() {
      return splits;
    }

    @Override
    public void setSplits(List<Split> splits) {
      this.splits = splits;
    }

    @Override
    public boolean doLog() {
      return doLog;
    }

    @Override
    public void setDoLog(boolean doLog) {
      this.doLog = doLog;
    }
  }
}

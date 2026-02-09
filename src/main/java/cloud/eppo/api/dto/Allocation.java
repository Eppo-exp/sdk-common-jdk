package cloud.eppo.api.dto;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Allocation {
  @NotNull String getKey();

  @Nullable Set<TargetingRule> getRules();

  @Nullable Date getStartAt();

  @Nullable Date getEndAt();

  @NotNull List<Split> getSplits();

  boolean doLog();

  class Default implements Allocation {
    private final String key;
    private final Set<TargetingRule> rules;
    private final Date startAt;
    private final Date endAt;
    private final List<Split> splits;
    private final boolean doLog;

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
    public Set<TargetingRule> getRules() {
      return rules;
    }

    @Override
    public Date getStartAt() {
      return startAt;
    }

    @Override
    public Date getEndAt() {
      return endAt;
    }

    @Override
    public List<Split> getSplits() {
      return splits;
    }

    @Override
    public boolean doLog() {
      return doLog;
    }
  }
}

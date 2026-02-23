package cloud.eppo.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Allocation extends Serializable {
  @NotNull String getKey();

  @Nullable Set<TargetingRule> getRules();

  @Nullable Date getStartAt();

  @Nullable Date getEndAt();

  @NotNull List<Split> getSplits();

  boolean doLog();

  class Default implements Allocation {
    private static final long serialVersionUID = 1L;
    private final @NotNull String key;
    private final @Nullable Set<TargetingRule> rules;
    private final @Nullable Date startAt;
    private final @Nullable Date endAt;
    private final @NotNull List<Split> splits;
    private final boolean doLog;

    public Default(
        @NotNull String key,
        @Nullable Set<TargetingRule> rules,
        @Nullable Date startAt,
        @Nullable Date endAt,
        @Nullable List<Split> splits,
        boolean doLog) {
      this.key = key;
      this.rules = rules == null ? null : Collections.unmodifiableSet(new HashSet<>(rules));
      this.startAt = startAt == null ? null : new Date(startAt.getTime());
      this.endAt = endAt == null ? null : new Date(endAt.getTime());
      this.splits =
          splits == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(splits));
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
    @NotNull public String getKey() {
      return key;
    }

    @Override
    @Nullable public Set<TargetingRule> getRules() {
      return rules;
    }

    @Override
    @Nullable public Date getStartAt() {
      return startAt == null ? null : new Date(startAt.getTime());
    }

    @Override
    @Nullable public Date getEndAt() {
      return endAt == null ? null : new Date(endAt.getTime());
    }

    @Override
    @NotNull public List<Split> getSplits() {
      return splits;
    }

    @Override
    public boolean doLog() {
      return doLog;
    }
  }
}

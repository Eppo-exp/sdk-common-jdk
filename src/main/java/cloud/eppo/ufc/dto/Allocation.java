package cloud.eppo.ufc.dto;

import static cloud.eppo.Utils.throwIfEmptyOrNull;
import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Allocation {
  @NotNull private String key;
  @NotNull private Set<TargetingRule> rules;
  @Nullable private Date startAt;
  @Nullable private Date endAt;
  @NotNull private List<Split> splits;
  private boolean doLog;

  public Allocation(
      @NotNull String key,
      @NotNull Set<TargetingRule> rules,
      @Nullable Date startAt,
      @Nullable Date endAt,
      @NotNull List<Split> splits,
      boolean doLog) {
    throwIfNull(key, "key must not be null");
    throwIfNull(rules, "rules must not be null");
    throwIfNull(splits, "splits must not be null");

    this.key = key;
    this.rules = rules;
    this.startAt = startAt;
    this.endAt = endAt;
    this.splits = splits;
    this.doLog = doLog;
  }

  @Override @NotNull
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
  public boolean equals(@Nullable Object o) {
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

  @NotNull
  public String getKey() {
    return key;
  }

  public void setKey(@NotNull String key) {
    throwIfEmptyOrNull(key, "key must not be null");

    this.key = key;
  }

  @NotNull
  public Set<TargetingRule> getRules() {
    return rules;
  }

  public void setRules(@NotNull Set<TargetingRule> rules) {
    throwIfNull(rules, "rules must not be null");

    this.rules = rules;
  }

  @Nullable
  public Date getStartAt() {
    return startAt;
  }

  public void setStartAt(@Nullable Date startAt) {
    this.startAt = startAt;
  }

  @Nullable
  public Date getEndAt() {
    return endAt;
  }

  public void setEndAt(@Nullable Date endAt) {
    this.endAt = endAt;
  }

  @NotNull
  public List<Split> getSplits() {
    return splits;
  }

  public void setSplits(@NotNull List<Split> splits) {
    throwIfNull(splits, "splits must not be null");

    this.splits = splits;
  }

  public boolean doLog() {
    return doLog;
  }

  public void setDoLog(boolean doLog) {
    this.doLog = doLog;
  }
}

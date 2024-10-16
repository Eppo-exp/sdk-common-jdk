package cloud.eppo.cache;

import java.util.Objects;

/**
 * Assignment cache keys are only on the subject and flag level, while a combination of keys and
 * fields are used for uniqueness checking. This way, if an assigned variation or bandit action
 * changes for a flag, it evicts the old one. Then, if an older assignment is later reassigned, it
 * will be treated as new.
 */
public class AssignmentCacheKey {
  private final String subjectKey;
  private final String flagKey;

  public AssignmentCacheKey(String subjectKey, String flagKey) {
    this.subjectKey = subjectKey;
    this.flagKey = flagKey;
  }

  public String getSubjectKey() {
    return subjectKey;
  }

  public String getFlagKey() {
    return flagKey;
  }

  @Override
  public String toString() {
    return subjectKey + ";" + flagKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AssignmentCacheKey that = (AssignmentCacheKey) o;
    return Objects.equals(toString(), that.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectKey, flagKey);
  }
}

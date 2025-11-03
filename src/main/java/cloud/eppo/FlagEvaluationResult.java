package cloud.eppo;

import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import cloud.eppo.api.Attributes;
import cloud.eppo.ufc.dto.FlagConfig;
import cloud.eppo.ufc.dto.Variation;
import java.util.Map;
import java.util.Objects;

public class FlagEvaluationResult {
  @NotNull private final FlagConfig flag;
  @NotNull private final String flagKey;
  @NotNull private final String subjectKey;
  @NotNull private final Attributes subjectAttributes;
  @Nullable private final FlagEvaluationAllocationKeyAndVariation allocationKeyAndVariation;
  @NotNull private final Map<String, String> extraLogging;
  private final boolean doLog;

  public FlagEvaluationResult(
      @NotNull FlagConfig flag,
      @NotNull String flagKey,
      @NotNull String subjectKey,
      @NotNull Attributes subjectAttributes,
      @Nullable FlagEvaluationAllocationKeyAndVariation allocationKeyAndVariation,
      @NotNull Map<String, String> extraLogging,
      boolean doLog) {
    throwIfNull(flag, "flag must not be null");
    throwIfNull(flagKey, "flagKey must not be null");
    throwIfNull(subjectKey, "subjectKey must not be null");
    throwIfNull(subjectAttributes, "subjectAttributes must not be null");
    throwIfNull(extraLogging, "extraLogging must not be null");

    this.flag = flag;
    this.flagKey = flagKey;
    this.subjectKey = subjectKey;
    this.subjectAttributes = subjectAttributes;
    this.allocationKeyAndVariation = allocationKeyAndVariation;
    this.extraLogging = extraLogging;
    this.doLog = doLog;
  }

  @Override @NotNull
  public String toString() {
    return "FlagEvaluationResult{" +
      "flag='" + flag + '\'' +
      ", flagKey='" + flagKey + '\'' +
      ", subjectKey='" + subjectKey + '\'' +
      ", subjectAttributes=" + subjectAttributes +
      ", allocationKeyAndVariation='" + allocationKeyAndVariation + '\'' +
      ", extraLogging=" + extraLogging +
      ", doLog=" + doLog +
      '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    FlagEvaluationResult that = (FlagEvaluationResult) o;
    return doLog == that.doLog
            && Objects.equals(flag, that.flag)
            && Objects.equals(flagKey, that.flagKey)
            && Objects.equals(subjectKey, that.subjectKey)
            && Objects.equals(subjectAttributes, that.subjectAttributes)
            && Objects.equals(allocationKeyAndVariation, that.allocationKeyAndVariation)
            && Objects.equals(extraLogging, that.extraLogging);
  }

  @Override
  public int hashCode() {
    return Objects.hash(flag, flagKey, subjectKey, subjectAttributes, allocationKeyAndVariation, extraLogging, doLog);
  }

  @NotNull
  public FlagConfig getFlag() {
    return flag;
  }

  @NotNull
  public String getFlagKey() {
    return flagKey;
  }

  @NotNull
  public String getSubjectKey() {
    return subjectKey;
  }

  @NotNull
  public Attributes getSubjectAttributes() {
    return subjectAttributes;
  }

  @Nullable
  public FlagEvaluationAllocationKeyAndVariation getAllocationKeyAndVariation() {
    return allocationKeyAndVariation;
  }

  @Nullable
  public String getAllocationKey() {
    return allocationKeyAndVariation != null ? allocationKeyAndVariation.allocationKey : null;
  }

  @Nullable
  public Variation getVariation() {
    return allocationKeyAndVariation != null ? allocationKeyAndVariation.variation : null;
  }

  @NotNull
  public Map<String, String> getExtraLogging() {
    return extraLogging;
  }

  public boolean doLog() {
    return doLog;
  }
}

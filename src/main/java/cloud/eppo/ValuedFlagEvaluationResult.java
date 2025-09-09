package cloud.eppo;

import static cloud.eppo.Utils.throwIfNull;
import static cloud.eppo.ValuedFlagEvaluationResultType.OK;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import cloud.eppo.api.EppoValue;

/**
 * The result of BaseEppoClient.getTypedAssignmentResult().
 * If BaseEppoClient can't call FlagEvaluator.evaluateFlag(),
 * then evaluationResult will be null.
 */
public class ValuedFlagEvaluationResult {
  @NotNull private final EppoValue value;
  @Nullable private final FlagEvaluationResult evaluationResult;
  @NotNull private final ValuedFlagEvaluationResultType type;

  public ValuedFlagEvaluationResult(
      @NotNull EppoValue value,
      @Nullable FlagEvaluationResult evaluationResult,
      @NotNull ValuedFlagEvaluationResultType type) {
    throwIfNull(value, "value must not be null");
    throwIfNull(type, "type must not be null");
    if (type == OK) {
      throwIfNull(evaluationResult, "evaluationResult must not be null if type is " + OK);
    }

    this.value = value;
    this.evaluationResult = evaluationResult;
    this.type = type;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ValuedFlagEvaluationResult that = (ValuedFlagEvaluationResult) o;
    return Objects.equals(value, that.value) && Objects.equals(evaluationResult, that.evaluationResult) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, evaluationResult, type);
  }

  @Override
  public String toString() {
    return "ValuedFlagEvaluationResult{" +
      "value=" + value +
      ", evaluationResult=" + evaluationResult +
      ", type=" + type +
      '}';
  }

  @NotNull
  public EppoValue getValue() {
    return value;
  }

  @Nullable
  public FlagEvaluationResult getEvaluationResult() {
    return evaluationResult;
  }

  @NotNull
  public ValuedFlagEvaluationResultType getType() {
    return type;
  }
}

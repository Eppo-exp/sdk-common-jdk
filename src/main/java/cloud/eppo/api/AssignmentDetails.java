package cloud.eppo.api;

/**
 * Contains both the assigned variation value and comprehensive evaluation details explaining why
 * that variation was assigned.
 *
 * @param <T> The type of the variation value (Boolean, Integer, Double, String, etc.)
 */
public class AssignmentDetails<T> {
  private final T variation;
  private final String action;
  private final EvaluationDetails evaluationDetails;

  public AssignmentDetails(T variation, String action, EvaluationDetails evaluationDetails) {
    this.variation = variation;
    this.action = action;
    this.evaluationDetails = evaluationDetails;
  }

  public T getVariation() {
    return variation;
  }

  public String getAction() {
    return action;
  }

  public EvaluationDetails getEvaluationDetails() {
    return evaluationDetails;
  }
}

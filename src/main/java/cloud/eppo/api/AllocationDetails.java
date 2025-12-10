package cloud.eppo.api;

/**
 * Details about an allocation evaluation, including its key, evaluation status, and position in the
 * allocation list.
 */
public class AllocationDetails {
  private final String key;
  private final AllocationEvaluationCode allocationEvaluationCode;
  private final int orderPosition;

  public AllocationDetails(
      String key, AllocationEvaluationCode allocationEvaluationCode, int orderPosition) {
    this.key = key;
    this.allocationEvaluationCode = allocationEvaluationCode;
    this.orderPosition = orderPosition;
  }

  public String getKey() {
    return key;
  }

  public AllocationEvaluationCode getAllocationEvaluationCode() {
    return allocationEvaluationCode;
  }

  public int getOrderPosition() {
    return orderPosition;
  }
}

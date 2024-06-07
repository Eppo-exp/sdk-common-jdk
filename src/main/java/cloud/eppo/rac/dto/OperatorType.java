package cloud.eppo.rac.dto;

/** Operation Supported */
public enum OperatorType {
  MATCHES("MATCHES"),
  GTE("GTE"),
  GT("GT"),
  LTE("LTE"),
  LT("LT"),
  ONE_OF("ONE_OF"),
  NOT_ONE_OF("NOT_ONE_OF");

  public final String label;

  OperatorType(String label) {
    this.label = label;
  }
}

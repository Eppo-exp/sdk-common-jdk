package cloud.eppo.ufc.dto;

import static cloud.eppo.Utils.getMD5Hex;

import java.util.HashMap;
import java.util.Map;

public enum OperatorType {
  NOT_MATCHES("NOT_MATCHES"),
  MATCHES("MATCHES"),
  GREATER_THAN_OR_EQUAL_TO("GTE"),
  GREATER_THAN("GT"),
  LESS_THAN_OR_EQUAL_TO("LTE"),
  LESS_THAN("LT"),
  ONE_OF("ONE_OF"),
  NOT_ONE_OF("NOT_ONE_OF"),
  IS_NULL("IS_NULL");

  public final String value;
  private static final Map<String, OperatorType> valuesToOperatorType =
      buildValueToOperatorTypeMap();
  private static final Map<String, OperatorType> hashesToOperatorType =
      buildHashToOperatorTypeMap();

  private static Map<String, OperatorType> buildValueToOperatorTypeMap() {
    Map<String, OperatorType> result = new HashMap<>();
    for (OperatorType type : OperatorType.values()) {
      result.put(type.value, type);
    }
    return result;
  }

  private static Map<String, OperatorType> buildHashToOperatorTypeMap() {
    Map<String, OperatorType> result = new HashMap<>();
    for (OperatorType type : OperatorType.values()) {
      result.put(getMD5Hex(type.value), type);
    }
    return result;
  }

  OperatorType(String value) {
    this.value = value;
  }

  public static OperatorType fromString(String value) {
    // First we try obfuscated lookup as in client situations we'll care more about ingestion
    // performance
    OperatorType type = hashesToOperatorType.get(value);
    // Then we'll try non-obfuscated lookup
    if (type == null) {
      type = valuesToOperatorType.get(value);
    }
    return type;
  }

  public boolean isInequalityComparison() {
    return this == GREATER_THAN_OR_EQUAL_TO
        || this == GREATER_THAN
        || this == LESS_THAN_OR_EQUAL_TO
        || this == LESS_THAN;
  }

  public boolean isListComparison() {
    return this == ONE_OF || this == NOT_ONE_OF;
  }
}

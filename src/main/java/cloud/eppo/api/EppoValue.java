package cloud.eppo.api;

import cloud.eppo.api.dto.EppoValueType;
import cloud.eppo.api.dto.VariationType;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class EppoValue {
  protected final EppoValueType type;
  protected Boolean boolValue;
  protected Double doubleValue;
  protected String stringValue;
  protected List<String> stringArrayValue;

  protected EppoValue() {
    this.type = EppoValueType.NULL;
  }

  protected EppoValue(boolean boolValue) {
    this.boolValue = boolValue;
    this.type = EppoValueType.BOOLEAN;
  }

  protected EppoValue(double doubleValue) {
    this.doubleValue = doubleValue;
    this.type = EppoValueType.NUMBER;
  }

  protected EppoValue(String stringValue) {
    this.stringValue = stringValue;
    this.type = EppoValueType.STRING;
  }

  protected EppoValue(List<String> stringArrayValue) {
    this.stringArrayValue = stringArrayValue;
    this.type = EppoValueType.ARRAY_OF_STRING;
  }

  public static EppoValue nullValue() {
    return new EppoValue();
  }

  public static EppoValue valueOf(boolean boolValue) {
    return new EppoValue(boolValue);
  }

  public static EppoValue valueOf(double doubleValue) {
    return new EppoValue(doubleValue);
  }

  public static EppoValue valueOf(String stringValue) {
    return new EppoValue(stringValue);
  }

  public static EppoValue valueOf(List<String> value) {
    return new EppoValue(value);
  }

  public boolean booleanValue() {
    return this.boolValue;
  }

  public double doubleValue() {
    return this.doubleValue;
  }

  public String stringValue() {
    return this.stringValue;
  }

  public List<String> stringArrayValue() {
    return this.stringArrayValue;
  }

  public boolean isNull() {
    return type == EppoValueType.NULL;
  }

  public boolean isBoolean() {
    return this.type == EppoValueType.BOOLEAN;
  }

  public boolean isNumeric() {
    return this.type == EppoValueType.NUMBER;
  }

  public boolean isString() {
    return this.type == EppoValueType.STRING;
  }

  public boolean isStringArray() {
    return type == EppoValueType.ARRAY_OF_STRING;
  }

  public EppoValueType getType() {
    return type;
  }

  /**
   * Unwraps this EppoValue to the appropriate Java type based on the variation type. For JSON
   * types, use {@link #unwrap(VariationType, Function)} instead.
   *
   * @param expectedType the expected variation type (must not be JSON)
   * @param <T> the target type (Boolean, Integer, Double, or String)
   * @return the unwrapped value
   * @throws IllegalArgumentException if expectedType is JSON (use the overload with jsonParser)
   */
  @SuppressWarnings("unchecked")
  public <T> T unwrap(VariationType expectedType) {
    if (expectedType == VariationType.JSON) {
      throw new IllegalArgumentException(
          "JSON unwrap requires a parser function; use unwrap(expectedType, jsonParser)");
    }
    return unwrapInternal(expectedType, null);
  }

  /**
   * Unwraps this EppoValue to the appropriate Java type based on the variation type, using the
   * provided parser for JSON values.
   *
   * @param expectedType the expected variation type
   * @param jsonParser function to parse JSON strings (required for JSON type, ignored otherwise)
   * @param <T> the target type (Boolean, Integer, Double, String, or the JSON parser's return type)
   * @return the unwrapped value
   */
  @SuppressWarnings("unchecked")
  public <T> T unwrap(VariationType expectedType, Function<String, ?> jsonParser) {
    return unwrapInternal(expectedType, jsonParser);
  }

  @SuppressWarnings("unchecked")
  private <T> T unwrapInternal(VariationType expectedType, Function<String, ?> jsonParser) {
    switch (expectedType) {
      case BOOLEAN:
        return (T) Boolean.valueOf(booleanValue());
      case INTEGER:
        return (T) Integer.valueOf(Double.valueOf(doubleValue()).intValue());
      case NUMERIC:
        return (T) Double.valueOf(doubleValue());
      case STRING:
        return (T) stringValue();
      case JSON:
        if (jsonParser == null) {
          throw new IllegalArgumentException("JSON parser required for JSON type");
        }
        return (T) jsonParser.apply(stringValue());
    }
    throw new IllegalArgumentException("Unknown variation type: " + expectedType);
  }

  @Override
  public String toString() {
    switch (this.type) {
      case BOOLEAN:
        return this.boolValue.toString();
      case NUMBER:
        return this.doubleValue.toString();
      case STRING:
        return this.stringValue;
      case ARRAY_OF_STRING:
        // Android21 back-compatibility
        return joinStringArray(this.stringArrayValue);
      case NULL:
        return "";
      default:
        throw new UnsupportedOperationException(
            "Cannot stringify Eppo Value type " + this.type.name());
    }
  }

  @Override
  public boolean equals(Object otherObject) {
    if (this == otherObject) {
      return true;
    }
    if (otherObject == null || getClass() != otherObject.getClass()) {
      return false;
    }
    EppoValue otherEppoValue = (EppoValue) otherObject;
    return type == otherEppoValue.type
        && Objects.equals(boolValue, otherEppoValue.boolValue)
        && Objects.equals(doubleValue, otherEppoValue.doubleValue)
        && Objects.equals(stringValue, otherEppoValue.stringValue)
        && Objects.equals(stringArrayValue, otherEppoValue.stringArrayValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, boolValue, doubleValue, stringValue, stringArrayValue);
  }

  /** This method is to allow for Android 21 support; String.join was introduced in API 26 */
  private static String joinStringArray(List<String> stringArray) {
    if (stringArray == null || stringArray.isEmpty()) {
      return "";
    }
    String delimiter = ", ";
    StringBuilder stringBuilder = new StringBuilder();
    Iterator<String> iterator = stringArray.iterator();
    while (iterator.hasNext()) {
      stringBuilder.append(iterator.next());
      if (iterator.hasNext()) {
        stringBuilder.append(delimiter);
      }
    }
    return stringBuilder.toString();
  }
}

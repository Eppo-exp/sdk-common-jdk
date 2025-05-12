package cloud.eppo.api;

import cloud.eppo.Utils;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Attributes extends HashMap<String, EppoValue> implements DiscriminableAttributes {
  public Attributes() {
    super();
  }

  public Attributes(Map<String, EppoValue> startingAttributes) {
    super(startingAttributes);
  }

  public EppoValue put(String key, String value) {
    return super.put(key, EppoValue.valueOf(value));
  }

  public EppoValue put(String key, int value) {
    return super.put(key, EppoValue.valueOf(value));
  }

  public EppoValue put(String key, long value) {
    return super.put(key, EppoValue.valueOf(value));
  }

  public EppoValue put(String key, float value) {
    return super.put(key, EppoValue.valueOf(value));
  }

  public EppoValue put(String key, double value) {
    return super.put(key, EppoValue.valueOf(value));
  }

  public EppoValue put(String key, boolean value) {
    return super.put(key, EppoValue.valueOf(value));
  }

  @Override
  public Attributes getNumericAttributes() {
    Map<String, EppoValue> numericValuesOnly =
        super.entrySet().stream()
            .filter(entry -> entry.getValue().isNumeric())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    return new Attributes(numericValuesOnly);
  }

  @Override
  public Attributes getCategoricalAttributes() {
    Map<String, EppoValue> nonNullNonNumericValuesOnly =
        super.entrySet().stream()
            .filter(entry -> !entry.getValue().isNumeric() && !entry.getValue().isNull())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    return new Attributes(nonNullNonNumericValuesOnly);
  }

  @Override
  public Attributes getAllAttributes() {
    return this;
  }

  /** Serializes the attributes to a JSON string, omitting attributes with a null value. */
  public String serializeNonNullAttributesToJSONString() {
    return Utils.serializeAttributesToJSONString(this, true);
  }
}

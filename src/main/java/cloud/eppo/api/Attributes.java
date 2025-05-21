package cloud.eppo.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
    return serializeAttributesToJSONString(true);
  }

  @SuppressWarnings("SameParameterValue")
  private String serializeAttributesToJSONString(boolean omitNulls) {
    Gson gson = new Gson();
    JsonObject result = new JsonObject();

    for (Map.Entry<String, EppoValue> entry : entrySet()) {
      String attributeName = entry.getKey();
      EppoValue attributeValue = entry.getValue();

      if (attributeValue == null || attributeValue.isNull()) {
        if (!omitNulls) {
          result.add(attributeName, null);
        }
      } else {
        if (attributeValue.isNumeric()) {
          result.addProperty(attributeName, attributeValue.doubleValue());
          continue;
        }
        if (attributeValue.isBoolean()) {
          result.addProperty(attributeName, attributeValue.booleanValue());
          continue;
        }
        // fall back put treating any other eppo values as a string
        result.addProperty(attributeName, attributeValue.toString());
      }
    }

    return gson.toJson(result);
  }
}

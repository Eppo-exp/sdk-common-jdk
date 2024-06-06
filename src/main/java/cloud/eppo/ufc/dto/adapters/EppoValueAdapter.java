package cloud.eppo.ufc.dto.adapters;

import cloud.eppo.ufc.dto.EppoValue;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EppoValueAdapter implements JsonDeserializer<EppoValue>, JsonSerializer<EppoValue> {
  private static final Logger log = LoggerFactory.getLogger(EppoValueAdapter.class);

  @Override
  public EppoValue deserialize(
      JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {

    EppoValue result;

    if (jsonElement == null || jsonElement.isJsonNull()) {
      result = EppoValue.nullValue();
    } else if (jsonElement.isJsonArray()) {
      List<String> stringArray = new ArrayList<>();
      for (JsonElement arrayElement : jsonElement.getAsJsonArray()) {
        if (arrayElement.isJsonPrimitive() && arrayElement.getAsJsonPrimitive().isString()) {
          stringArray.add(arrayElement.getAsJsonPrimitive().getAsString());
        } else {
          log.warn(
              "only Strings are supported for array-valued values; received: {}", arrayElement);
        }
      }
      result = EppoValue.valueOf(stringArray);
    } else if (jsonElement.isJsonPrimitive()) {
      JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
      if (jsonPrimitive.isBoolean()) {
        result = EppoValue.valueOf(jsonPrimitive.getAsBoolean());
      } else if (jsonPrimitive.isNumber()) {
        result = EppoValue.valueOf(jsonPrimitive.getAsDouble());
      } else {
        result = EppoValue.valueOf(jsonPrimitive.getAsString());
      }
    } else {
      // If here, we don't know what to do; fail to null with a warning
      log.warn("Unexpected JSON for parsing a value: {}", jsonElement);
      result = EppoValue.nullValue();
    }

    return result;
  }

  @Override
  public JsonElement serialize(EppoValue src, Type typeOfSrc, JsonSerializationContext context) {
    if (src.isBoolean()) {
      return new JsonPrimitive(src.booleanValue());
    }

    if (src.isNumeric()) {
      return new JsonPrimitive(src.doubleValue());
    }

    if (src.isString()) {
      return new JsonPrimitive(src.stringValue());
    }

    if (src.isStringArray()) {
      JsonArray array = new JsonArray();
      for (String value : src.stringArrayValue()) {
        array.add(value);
      }
      return array;
    }

    return JsonNull.INSTANCE;
  }
}

package cloud.eppo.ufc.dto.adapters;

import static cloud.eppo.Utils.parseUtcISODateNode;

import cloud.eppo.api.EppoValue;
import cloud.eppo.model.ShardRange;
import cloud.eppo.ufc.dto.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GsonAdapter {

  private static final Logger log = LoggerFactory.getLogger(GsonAdapter.class);

  public static Gson createGson() {
    GsonBuilder gsonBuilder = new GsonBuilder();

    // Register type adapters for Eppo-specific classes
    gsonBuilder.registerTypeAdapter(FlagConfigResponse.class, new FlagConfigResponseDeserializer());
    gsonBuilder.registerTypeAdapter(
        BanditParametersResponse.class, new BanditParametersResponseDeserializer());
    gsonBuilder.registerTypeAdapter(EppoValue.class, new EppoValueTypeAdapter());
    gsonBuilder.registerTypeAdapter(Date.class, new DateAdapter());

    return gsonBuilder.create();
  }

  /** Custom GSON deserializer for BanditParametersResponse objects. */
  private static class BanditParametersResponseDeserializer
      implements JsonDeserializer<BanditParametersResponse> {

    @Override
    public BanditParametersResponse deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject rootNode = json.getAsJsonObject();

      if (rootNode == null) {
        log.warn("no top-level JSON object");
        return new BanditParametersResponse();
      }

      Map<String, BanditParameters> bandits = new ConcurrentHashMap<>();
      JsonElement banditsNode = rootNode.get("bandits");

      if (banditsNode != null && banditsNode.isJsonObject()) {
        JsonObject banditsObj = banditsNode.getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : banditsObj.entrySet()) {
          JsonObject banditNode = entry.getValue().getAsJsonObject();

          String modelVersion = banditNode.get("modelVersion").getAsString();
          String modelName =
              banditNode.has("modelName") ? banditNode.get("modelName").getAsString() : "";
          Date updatedAt = new Date(); // Use current time as default
          String banditKey = entry.getKey(); // Use the map key as the bandit key
          Map<String, Object> modelData =
              deserializeModelData(banditNode.get("modelData").getAsJsonObject());
          BanditModelData banditModelData = new BanditModelData(modelData);

          bandits.put(
              entry.getKey(),
              new BanditParameters(banditKey, updatedAt, modelName, modelVersion, banditModelData));
        }
      }

      return new BanditParametersResponse(bandits);
    }

    private Map<String, Object> deserializeModelData(JsonObject jsonObject) {
      Map<String, Object> result = new HashMap<>();

      for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
        String key = entry.getKey();
        JsonElement value = entry.getValue();

        if (value.isJsonNull()) {
          result.put(key, null);
        } else if (value.isJsonPrimitive()) {
          if (value.getAsJsonPrimitive().isBoolean()) {
            result.put(key, value.getAsBoolean());
          } else if (value.getAsJsonPrimitive().isNumber()) {
            result.put(key, value.getAsDouble());
          } else {
            result.put(key, value.getAsString());
          }
        } else if (value.isJsonArray()) {
          List<Object> list = new ArrayList<>();
          for (JsonElement element : value.getAsJsonArray()) {
            if (element.isJsonObject()) {
              list.add(deserializeModelData(element.getAsJsonObject()));
            } else if (element.isJsonPrimitive()) {
              if (element.getAsJsonPrimitive().isBoolean()) {
                list.add(element.getAsBoolean());
              } else if (element.getAsJsonPrimitive().isNumber()) {
                list.add(element.getAsDouble());
              } else {
                list.add(element.getAsString());
              }
            }
          }
          result.put(key, list);
        } else if (value.isJsonObject()) {
          result.put(key, deserializeModelData(value.getAsJsonObject()));
        }
      }

      return result;
    }
  }

  /** Custom GSON deserializer for FlagConfigResponse objects. */
  private static class FlagConfigResponseDeserializer
      implements JsonDeserializer<FlagConfigResponse> {
    private final EppoValueTypeAdapter eppoValueAdapter = new EppoValueTypeAdapter();

    @Override
    public FlagConfigResponse deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject rootNode = json.getAsJsonObject();

      if (rootNode == null) {
        log.warn("no top-level JSON object");
        return new FlagConfigResponse();
      }

      JsonElement flagsNode = rootNode.get("flags");
      if (flagsNode == null || !flagsNode.isJsonObject()) {
        log.warn("no root-level flags object");
        return new FlagConfigResponse();
      }

      // Default is to assume that the config is not obfuscated.
      JsonElement formatNode = rootNode.get("format");
      FlagConfigResponse.Format dataFormat =
          formatNode == null
              ? FlagConfigResponse.Format.SERVER
              : FlagConfigResponse.Format.valueOf(formatNode.getAsString());

      Map<String, FlagConfig> flags = new ConcurrentHashMap<>();

      JsonObject flagsObj = flagsNode.getAsJsonObject();
      for (Map.Entry<String, JsonElement> entry : flagsObj.entrySet()) {
        FlagConfig flagConfig = deserializeFlag(entry.getValue().getAsJsonObject());
        flags.put(entry.getKey(), flagConfig);
      }

      Map<String, BanditReference> banditReferences = new ConcurrentHashMap<>();
      if (rootNode.has("banditReferences")) {
        JsonElement banditReferencesNode = rootNode.get("banditReferences");
        if (!banditReferencesNode.isJsonObject()) {
          log.warn("root-level banditReferences property is present but not a JSON object");
        } else {
          JsonObject banditReferencesObj = banditReferencesNode.getAsJsonObject();
          for (Map.Entry<String, JsonElement> entry : banditReferencesObj.entrySet()) {
            BanditReference banditReference =
                deserializeBanditReference(entry.getValue().getAsJsonObject());
            banditReferences.put(entry.getKey(), banditReference);
          }
        }
      }

      return new FlagConfigResponse(flags, banditReferences, dataFormat);
    }

    private FlagConfig deserializeFlag(JsonObject jsonNode) {
      String key = jsonNode.get("key").getAsString();
      boolean enabled = jsonNode.get("enabled").getAsBoolean();
      int totalShards = jsonNode.get("totalShards").getAsInt();
      VariationType variationType =
          VariationType.fromString(jsonNode.get("variationType").getAsString());
      Map<String, Variation> variations =
          deserializeVariations(jsonNode.get("variations").getAsJsonObject());
      List<Allocation> allocations =
          deserializeAllocations(jsonNode.get("allocations").getAsJsonArray());

      return new FlagConfig(key, enabled, totalShards, variationType, variations, allocations);
    }

    private Map<String, Variation> deserializeVariations(JsonObject jsonNode) {
      Map<String, Variation> variations = new HashMap<>();
      if (jsonNode == null) {
        return variations;
      }

      for (Map.Entry<String, JsonElement> entry : jsonNode.entrySet()) {
        JsonObject variationObj = entry.getValue().getAsJsonObject();
        String key = variationObj.get("key").getAsString();
        EppoValue value = eppoValueAdapter.parseJsonElement(variationObj.get("value"));
        variations.put(entry.getKey(), new Variation(key, value));
      }
      return variations;
    }

    private List<Allocation> deserializeAllocations(JsonElement jsonNode) {
      List<Allocation> allocations = new ArrayList<>();
      if (jsonNode == null) {
        return allocations;
      }

      for (JsonElement allocation : jsonNode.getAsJsonArray()) {
        JsonObject allocationObj = allocation.getAsJsonObject();
        String key = allocationObj.get("key").getAsString();
        Set<TargetingRule> rules = deserializeTargetingRules(allocationObj.get("rules"));
        Date startAt = parseUtcISODateNode(allocationObj.get("startAt"));
        Date endAt = parseUtcISODateNode(allocationObj.get("endAt"));
        List<Split> splits = deserializeSplits(allocationObj.get("splits"));
        boolean doLog = allocationObj.get("doLog").getAsBoolean();
        allocations.add(new Allocation(key, rules, startAt, endAt, splits, doLog));
      }
      return allocations;
    }

    private Set<TargetingRule> deserializeTargetingRules(JsonElement jsonNode) {
      Set<TargetingRule> targetingRules = new HashSet<>();
      if (jsonNode == null || !jsonNode.isJsonArray()) {
        return targetingRules;
      }

      for (JsonElement rule : jsonNode.getAsJsonArray()) {
        JsonObject ruleObj = rule.getAsJsonObject();
        Set<TargetingCondition> conditions = new HashSet<>();

        for (JsonElement condition : ruleObj.get("conditions").getAsJsonArray()) {
          JsonObject conditionObj = condition.getAsJsonObject();
          String attribute = conditionObj.get("attribute").getAsString();
          String operatorKey = conditionObj.get("operator").getAsString();
          OperatorType operator = OperatorType.fromString(operatorKey);
          if (operator == null) {
            log.warn("Unknown operator \"{}\"", operatorKey);
            continue;
          }
          EppoValue value = eppoValueAdapter.parseJsonElement(conditionObj.get("value"));
          conditions.add(new TargetingCondition(operator, attribute, value));
        }
        targetingRules.add(new TargetingRule(conditions));
      }

      return targetingRules;
    }

    private List<Split> deserializeSplits(JsonElement jsonNode) {
      List<Split> splits = new ArrayList<>();
      if (jsonNode == null || !jsonNode.isJsonArray()) {
        return splits;
      }

      for (JsonElement split : jsonNode.getAsJsonArray()) {
        JsonObject splitObj = split.getAsJsonObject();
        String variationKey = splitObj.get("variationKey").getAsString();
        Set<Shard> shards = deserializeShards(splitObj.get("shards"));

        Map<String, String> extraLogging = new HashMap<>();
        JsonElement extraLoggingNode = splitObj.get("extraLogging");
        if (extraLoggingNode != null && extraLoggingNode.isJsonObject()) {
          JsonObject extraLoggingObj = extraLoggingNode.getAsJsonObject();
          for (Map.Entry<String, JsonElement> entry : extraLoggingObj.entrySet()) {
            extraLogging.put(entry.getKey(), entry.getValue().getAsString());
          }
        }
        splits.add(new Split(variationKey, shards, extraLogging));
      }

      return splits;
    }

    private Set<Shard> deserializeShards(JsonElement jsonNode) {
      Set<Shard> shards = new HashSet<>();
      if (jsonNode == null || !jsonNode.isJsonArray()) {
        return shards;
      }

      for (JsonElement shard : jsonNode.getAsJsonArray()) {
        JsonObject shardObj = shard.getAsJsonObject();
        String salt = shardObj.get("salt").getAsString();
        Set<ShardRange> ranges = new HashSet<>();

        for (JsonElement range : shardObj.get("ranges").getAsJsonArray()) {
          JsonObject rangeObj = range.getAsJsonObject();
          int start = rangeObj.get("start").getAsInt();
          int end = rangeObj.get("end").getAsInt();
          ranges.add(new ShardRange(start, end));
        }
        shards.add(new Shard(salt, ranges));
      }
      return shards;
    }

    private BanditReference deserializeBanditReference(JsonObject jsonNode) {
      String modelVersion = jsonNode.get("modelVersion").getAsString();
      List<BanditFlagVariation> flagVariations = new ArrayList<>();
      JsonElement flagVariationsNode = jsonNode.get("flagVariations");

      if (flagVariationsNode != null && flagVariationsNode.isJsonArray()) {
        for (JsonElement flagVariation : flagVariationsNode.getAsJsonArray()) {
          JsonObject flagVariationObj = flagVariation.getAsJsonObject();
          String banditKey = flagVariationObj.get("key").getAsString();
          String flagKey = flagVariationObj.get("flagKey").getAsString();
          String variationValue = flagVariationObj.get("variationValue").getAsString();

          // Default values for allocation key and variation key
          String allocationKey =
              flagVariationObj.has("allocationKey")
                  ? flagVariationObj.get("allocationKey").getAsString()
                  : "";
          String variationKey =
              flagVariationObj.has("variationKey")
                  ? flagVariationObj.get("variationKey").getAsString()
                  : "";

          flagVariations.add(
              new BanditFlagVariation(
                  banditKey, flagKey, allocationKey, variationKey, variationValue));
        }
      }

      return new BanditReference(modelVersion, flagVariations);
    }
  }

  /** Custom GSON TypeAdapter for EppoValue objects. */
  private static class EppoValueTypeAdapter extends TypeAdapter<EppoValue> {
    @Override
    public void write(JsonWriter out, EppoValue value) throws IOException {
      if (value == null || value.isNull()) {
        out.nullValue();
        return;
      }

      if (value.isBoolean()) {
        out.value(value.booleanValue());
      } else if (value.isNumeric()) {
        out.value(value.doubleValue());
      } else if (value.isString()) {
        out.value(value.stringValue());
      } else if (value.isStringArray()) {
        out.beginArray();
        for (String item : value.stringArrayValue()) {
          out.value(item);
        }
        out.endArray();
      }
    }

    @Override
    public EppoValue read(JsonReader in) throws IOException {
      throw new UnsupportedOperationException("Use parseJsonElement instead");
    }

    // Parse an EppoValue from a JsonElement - renamed from fromJsonTree to avoid final method
    // conflict
    public EppoValue parseJsonElement(JsonElement json) {
      if (json == null || json.isJsonNull()) {
        return EppoValue.nullValue();
      }

      if (json.isJsonArray()) {
        List<String> stringArray = new ArrayList<>();
        for (JsonElement arrayElement : json.getAsJsonArray()) {
          if (arrayElement.isJsonPrimitive() && arrayElement.getAsJsonPrimitive().isString()) {
            stringArray.add(arrayElement.getAsString());
          } else {
            log.warn(
                "only Strings are supported for array-valued values; received: {}", arrayElement);
          }
        }
        return EppoValue.valueOf(stringArray);
      } else if (json.isJsonPrimitive()) {
        if (json.getAsJsonPrimitive().isBoolean()) {
          return EppoValue.valueOf(json.getAsBoolean());
        } else if (json.getAsJsonPrimitive().isNumber()) {
          return EppoValue.valueOf(json.getAsDouble());
        } else {
          return EppoValue.valueOf(json.getAsString());
        }
      } else {
        // If here, we don't know what to do; fail to null with a warning
        log.warn("Unexpected JSON for parsing a value: {}", json);
        return EppoValue.nullValue();
      }
    }
  }

  /** Custom GSON serializer/deserializer for Date objects. */
  private static class DateAdapter implements JsonDeserializer<Date> {
    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return parseUtcISODateNode(json);
    }
  }
}

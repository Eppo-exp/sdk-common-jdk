package cloud.eppo.ufc.dto.adapters;

import static cloud.eppo.Utils.parseUtcISODateElement;

import cloud.eppo.model.ShardRange;
import cloud.eppo.ufc.dto.*;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hand-rolled deserializer so that we don't rely on annotations and method names, which can be
 * unreliable when ProGuard minification is in-use and not configured to protect
 * JSON-deserialization-related classes and annotations.
 */
public class FlagConfigResponseDeserializer extends StdDeserializer<FlagConfigResponse> {
  private static final Logger log = LoggerFactory.getLogger(FlagConfigResponseDeserializer.class);
  private final EppoValueDeserializer eppoValueDeserializer = new EppoValueDeserializer();

  protected FlagConfigResponseDeserializer(Class<?> vc) {
    super(vc);
  }

  public FlagConfigResponseDeserializer() {
    this(null);
  }

  @Override
  public FlagConfigResponse deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException, JacksonException {
    JsonNode rootElement = jp.getCodec().readTree(jp);

    if (rootElement == null || !rootElement.isObject()) {
      log.warn("no top-level JSON object");
      return new FlagConfigResponse();
    }
    ObjectNode rootObject = (ObjectNode) rootElement;
    JsonNode flagsElement = rootObject.get("flags");
    if (flagsElement == null) {
      log.warn("no root-level flags property");
      return new FlagConfigResponse();
    }
    Map<String, FlagConfig> flags = new ConcurrentHashMap<>();
    ObjectNode flagsObject = (ObjectNode) flagsElement;
    for (Map.Entry<String, JsonNode> flagEntry : flagsObject.properties()) {
      FlagConfig flagConfig = deserializeFlag(flagEntry.getValue(), ctxt);
      flags.put(flagEntry.getKey(), flagConfig);
    }

    return new FlagConfigResponse(flags);
  }

  private FlagConfig deserializeFlag(JsonNode jsonNode, DeserializationContext context) {
    String key = jsonNode.get("key").asText();
    boolean enabled = jsonNode.get("enabled").asBoolean();
    int totalShards = jsonNode.get("totalShards").asInt();
    VariationType variationType = VariationType.fromString(jsonNode.get("variationType").asText());
    Map<String, Variation> variations = deserializeVariations(jsonNode.get("variations"), context);
    List<Allocation> allocations = deserializeAllocations(jsonNode.get("allocations"), context);

    return new FlagConfig(key, enabled, totalShards, variationType, variations, allocations);
  }

  private Map<String, Variation> deserializeVariations(
      JsonNode jsonNode, DeserializationContext context) {
    Map<String, Variation> variations = new HashMap<>();
    if (jsonNode == null) {
      return variations;
    }
    for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = it.next();
      String key = entry.getValue().get("key").asText();
      EppoValue value = eppoValueDeserializer.deserializeNode(entry.getValue().get("value"));
      variations.put(entry.getKey(), new Variation(key, value));
    }
    return variations;
  }

  private List<Allocation> deserializeAllocations(JsonNode jsonNode, DeserializationContext ctxt) {
    List<Allocation> allocations = new ArrayList<>();
    if (jsonNode == null) {
      return allocations;
    }
    for (JsonNode allocationNode : jsonNode) {
      String key = allocationNode.get("key").asText();
      Set<TargetingRule> rules = deserializeTargetingRules(allocationNode.get("rules"), ctxt);
      Date startAt = parseUtcISODateElement(allocationNode.get("startAt"));
      Date endAt = parseUtcISODateElement(allocationNode.get("endAt"));
      List<Split> splits = deserializeSplits(allocationNode.get("splits"));
      boolean doLog = allocationNode.get("doLog").asBoolean();
      allocations.add(new Allocation(key, rules, startAt, endAt, splits, doLog));
    }
    return allocations;
  }

  private Set<TargetingRule> deserializeTargetingRules(
      JsonNode jsonNode, DeserializationContext context) {
    Set<TargetingRule> targetingRules = new HashSet<>();
    if (jsonNode == null || !jsonNode.isArray()) {
      return targetingRules;
    }
    for (JsonNode ruleNode : jsonNode) {
      Set<TargetingCondition> conditions = new HashSet<>();
      for (JsonNode conditionNode : ruleNode.get("conditions")) {
        String attribute = conditionNode.get("attribute").asText();
        String operatorKey = conditionNode.get("operator").asText();
        OperatorType operator = OperatorType.fromString(operatorKey);
        if (operator == null) {
          log.warn("Unknown operator \"{}\"", operatorKey);
          continue;
        }
        EppoValue value = eppoValueDeserializer.deserializeNode(conditionNode.get("value"));
        conditions.add(new TargetingCondition(operator, attribute, value));
      }
      targetingRules.add(new TargetingRule(conditions));
    }

    return targetingRules;
  }

  private List<Split> deserializeSplits(JsonNode jsonNode) {
    List<Split> splits = new ArrayList<>();
    if (jsonNode == null || !jsonNode.isArray()) {
      return splits;
    }
    for (JsonNode splitNode : jsonNode) {
      String variationKey = splitNode.get("variationKey").asText();
      Set<Shard> shards = deserializeShards(splitNode.get("shards"));
      Map<String, String> extraLogging = new HashMap<>();
      JsonNode extraLoggingNode = splitNode.get("extraLogging");
      if (extraLoggingNode != null && extraLoggingNode.isObject()) {
        for (Iterator<Map.Entry<String, JsonNode>> it = extraLoggingNode.fields(); it.hasNext(); ) {
          Map.Entry<String, JsonNode> entry = it.next();
          extraLogging.put(entry.getKey(), entry.getValue().asText());
        }
      }
      splits.add(new Split(variationKey, shards, extraLogging));
    }

    return splits;
  }

  private Set<Shard> deserializeShards(JsonNode jsonNode) {
    Set<Shard> shards = new HashSet<>();
    if (jsonNode == null || !jsonNode.isArray()) {
      return shards;
    }
    for (JsonNode shardNode : jsonNode) {
      String salt = shardNode.get("salt").asText();
      Set<ShardRange> ranges = new HashSet<>();
      for (JsonNode rangeNode : shardNode.get("ranges")) {
        int start = rangeNode.get("start").asInt();
        int end = rangeNode.get("end").asInt();
        ranges.add(new ShardRange(start, end));
      }
      shards.add(new Shard(salt, ranges));
    }
    return shards;
  }
}

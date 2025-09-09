package cloud.eppo.ufc.dto.adapters;

import static cloud.eppo.Utils.parseUtcISODateNode;

import cloud.eppo.api.EppoValue;
import cloud.eppo.model.ShardRange;
import cloud.eppo.ufc.dto.*;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    JsonNode rootNode = jp.getCodec().readTree(jp);

    if (rootNode == null || !rootNode.isObject()) {
      log.warn("no top-level JSON object");
      return new FlagConfigResponse();
    }
    JsonNode flagsNode = rootNode.get("flags");
    if (flagsNode == null || !flagsNode.isObject()) {
      log.warn("no root-level flags object");
      return new FlagConfigResponse();
    }

    // Default is to assume that the config is not obfuscated.
    JsonNode formatNode = rootNode.get("format");
    FlagConfigResponse.Format dataFormat =
        formatNode == null
            ? FlagConfigResponse.Format.SERVER
            : FlagConfigResponse.Format.valueOf(formatNode.asText());

    Map<String, FlagConfig> flags = new ConcurrentHashMap<>();

    flagsNode
        .fields()
        .forEachRemaining(
            field -> {
              FlagConfig flagConfig = deserializeFlag(field.getValue());
              flags.put(field.getKey(), flagConfig);
            });

    Map<String, BanditReference> banditReferences = new ConcurrentHashMap<>();
    if (rootNode.has("banditReferences")) {
      JsonNode banditReferencesNode = rootNode.get("banditReferences");
      if (!banditReferencesNode.isObject()) {
        log.warn("root-level banditReferences property is present but not a JSON object");
      } else {
        banditReferencesNode
            .fields()
            .forEachRemaining(
                field -> {
                  BanditReference banditReference = deserializeBanditReference(field.getValue());
                  banditReferences.put(field.getKey(), banditReference);
                });
      }
    }

    return new FlagConfigResponse(flags, banditReferences, dataFormat);
  }

  @NotNull
  private FlagConfig deserializeFlag(@NotNull JsonNode jsonNode) {
    @NotNull final String key = jsonNode.get("key").asText();
    boolean enabled = jsonNode.get("enabled").asBoolean();
    int totalShards = jsonNode.get("totalShards").asInt();
    @Nullable final VariationType variationType = VariationType.fromString(jsonNode.get("variationType").asText());
    @NotNull final Map<String, Variation> variations = deserializeVariations(jsonNode.get("variations"));
    @NotNull final List<String> sortedVariationKeys = new ArrayList<>(variations.keySet());
    Collections.sort(sortedVariationKeys);
    @NotNull final List<Allocation> allocations = deserializeAllocations(jsonNode.get("allocations"));

    return new FlagConfig(key, enabled, totalShards, variationType, variations, sortedVariationKeys, allocations);
  }

  @NotNull
  private Map<String, Variation> deserializeVariations(@Nullable JsonNode jsonNode) {
    @NotNull final Map<String, Variation> variations = new HashMap<>();
    if (jsonNode == null) {
      return variations;
    }
    for (@NotNull final Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); ) {
      @NotNull final Map.Entry<String, JsonNode> entry = it.next();
      @NotNull final String key = entry.getValue().get("key").asText();
      @NotNull final EppoValue value = eppoValueDeserializer.deserializeNode(entry.getValue().get("value"));
      variations.put(entry.getKey(), new Variation(key, value));
    }
    return variations;
  }

  @NotNull
  private List<Allocation> deserializeAllocations(@Nullable JsonNode jsonNode) {
    @NotNull final List<Allocation> allocations = new ArrayList<>();
    if (jsonNode == null) {
      return allocations;
    }
    for (@NotNull final JsonNode allocationNode : jsonNode) {
      @NotNull final String key = allocationNode.get("key").asText();
      @NotNull final Set<TargetingRule> rules = deserializeTargetingRules(allocationNode.get("rules"));
      @Nullable final Date startAt = parseUtcISODateNode(allocationNode.get("startAt"));
      @Nullable final Date endAt = parseUtcISODateNode(allocationNode.get("endAt"));
      @NotNull final List<Split> splits = deserializeSplits(allocationNode.get("splits"));
      boolean doLog = allocationNode.get("doLog").asBoolean();
      allocations.add(new Allocation(key, rules, startAt, endAt, splits, doLog));
    }
    return allocations;
  }

  @NotNull
  private Set<TargetingRule> deserializeTargetingRules(@Nullable JsonNode jsonNode) {
    @NotNull final Set<TargetingRule> targetingRules = new HashSet<>();
    if (jsonNode == null || !jsonNode.isArray()) {
      return targetingRules;
    }
    for (@NotNull final JsonNode ruleNode : jsonNode) {
      @NotNull final Set<TargetingCondition> conditions = new HashSet<>();
      for (@NotNull final JsonNode conditionNode : ruleNode.get("conditions")) {
        @NotNull final String attribute = conditionNode.get("attribute").asText();
        @NotNull final String operatorKey = conditionNode.get("operator").asText();
        @Nullable final OperatorType operator = OperatorType.fromString(operatorKey);
        if (operator == null) {
          log.warn("Unknown operator \"{}\"", operatorKey);
          continue;
        }
        @NotNull final EppoValue value = eppoValueDeserializer.deserializeNode(conditionNode.get("value"));
        conditions.add(new TargetingCondition(operator, attribute, value));
      }
      targetingRules.add(new TargetingRule(conditions));
    }

    return targetingRules;
  }

  @NotNull
  private List<Split> deserializeSplits(@Nullable JsonNode jsonNode) {
    @NotNull final List<Split> splits = new ArrayList<>();
    if (jsonNode == null || !jsonNode.isArray()) {
      return splits;
    }
    for (@NotNull final JsonNode splitNode : jsonNode) {
      @NotNull final String variationKey = splitNode.get("variationKey").asText();
      @NotNull final Set<Shard> shards = deserializeShards(splitNode.get("shards"));
      @NotNull final Map<String, String> extraLogging = new HashMap<>();
      @Nullable final JsonNode extraLoggingNode = splitNode.get("extraLogging");
      if (extraLoggingNode != null && extraLoggingNode.isObject()) {
        for (@NotNull final Iterator<Map.Entry<String, JsonNode>> it = extraLoggingNode.fields(); it.hasNext(); ) {
          @NotNull final Map.Entry<String, JsonNode> entry = it.next();
          extraLogging.put(entry.getKey(), entry.getValue().asText());
        }
      }
      splits.add(new Split(variationKey, shards, extraLogging));
    }

    return splits;
  }

  @NotNull
  private Set<Shard> deserializeShards(@Nullable JsonNode jsonNode) {
    @NotNull final Set<Shard> shards = new HashSet<>();
    if (jsonNode == null || !jsonNode.isArray()) {
      return shards;
    }
    for (@NotNull final JsonNode shardNode : jsonNode) {
      @NotNull final String salt = shardNode.get("salt").asText();
      @NotNull final Set<ShardRange> ranges = new HashSet<>();
      for (@NotNull final JsonNode rangeNode : shardNode.get("ranges")) {
        final int start = rangeNode.get("start").asInt();
        final int end = rangeNode.get("end").asInt();
        ranges.add(new ShardRange(start, end));
      }
      shards.add(new Shard(salt, ranges));
    }
    return shards;
  }

  @NotNull
  private BanditReference deserializeBanditReference(@NotNull JsonNode jsonNode) {
    @NotNull final String modelVersion = jsonNode.get("modelVersion").asText();
    @NotNull final List<BanditFlagVariation> flagVariations = new ArrayList<>();
    @Nullable final JsonNode flagVariationsNode = jsonNode.get("flagVariations");
    if (flagVariationsNode != null && flagVariationsNode.isArray()) {
      for (@NotNull final JsonNode flagVariationNode : flagVariationsNode) {
        @NotNull final String banditKey = flagVariationNode.get("key").asText();
        @NotNull final String flagKey = flagVariationNode.get("flagKey").asText();
        @NotNull final String allocationKey = flagVariationNode.get("allocationKey").asText();
        @NotNull final String variationKey = flagVariationNode.get("variationKey").asText();
        @NotNull final String variationValue = flagVariationNode.get("variationValue").asText();
        @NotNull final BanditFlagVariation flagVariation =
            new BanditFlagVariation(
                banditKey, flagKey, allocationKey, variationKey, variationValue);
        flagVariations.add(flagVariation);
      }
    }
    return new BanditReference(modelVersion, flagVariations);
  }
}

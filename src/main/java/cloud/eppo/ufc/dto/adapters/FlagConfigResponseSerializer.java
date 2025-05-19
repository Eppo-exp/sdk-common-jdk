package cloud.eppo.ufc.dto.adapters;

import static cloud.eppo.Utils.getISODate;
import static cloud.eppo.Utils.parseUtcISODateNode;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import cloud.eppo.api.EppoValue;
import cloud.eppo.model.ShardRange;
import cloud.eppo.ufc.dto.Allocation;
import cloud.eppo.ufc.dto.BanditFlagVariation;
import cloud.eppo.ufc.dto.BanditReference;
import cloud.eppo.ufc.dto.FlagConfig;
import cloud.eppo.ufc.dto.FlagConfigResponse;
import cloud.eppo.ufc.dto.OperatorType;
import cloud.eppo.ufc.dto.Shard;
import cloud.eppo.ufc.dto.Split;
import cloud.eppo.ufc.dto.TargetingCondition;
import cloud.eppo.ufc.dto.TargetingRule;
import cloud.eppo.ufc.dto.Variation;
import cloud.eppo.ufc.dto.VariationType;

/**
 * Hand-rolled serializer so that we don't rely on annotations and method names, which can be
 * unreliable when ProGuard minification is in-use and not configured to protect
 * JSON-serialization-related classes and annotations.
 */
public class FlagConfigResponseSerializer extends StdSerializer<FlagConfigResponse> {
  private static final Logger log = LoggerFactory.getLogger(FlagConfigResponseSerializer.class);
  private final EppoValueSerializer eppoValueSerializer = new EppoValueSerializer();

  protected FlagConfigResponseSerializer(Class<FlagConfigResponse> vc) {
    super(vc);
  }

  public FlagConfigResponseSerializer() {
    this(null);
  }

  @Override
  public void serialize(FlagConfigResponse src, JsonGenerator jgen, SerializerProvider provider)
      throws IOException, JacksonException {
    jgen.writeStartObject();
    final FlagConfigResponse.Format format = src.getFormat();
    if (format != null) {
      jgen.writeStringField("format", format.name());
    }
    final Map<String, FlagConfig> flags = src.getFlags();
    if (flags != null) {
      jgen.writeFieldName("flags");
      jgen.writeStartObject();
      for (Map.Entry<String, FlagConfig> entry : src.getFlags().entrySet()) {
        jgen.writeFieldName(entry.getKey());
        serializeFlag(entry.getValue(), jgen, provider);
      }
      jgen.writeEndObject();
    }
    final Map<String, BanditReference> banditReferences = src.getBanditReferences();
    if (banditReferences != null) {
      jgen.writeFieldName("banditReferences");
      jgen.writeStartObject();
      for (Map.Entry<String, BanditReference> entry : banditReferences.entrySet()) {
        jgen.writeFieldName(entry.getKey());
        serializeBanditReference(entry.getValue(), jgen);
      }
      jgen.writeEndObject();
    }
    jgen.writeEndObject();
  }

  private void serializeFlag(FlagConfig flagConfig, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("key", flagConfig.getKey());
    jgen.writeBooleanField("enabled", flagConfig.isEnabled());
    jgen.writeNumberField("totalShards", flagConfig.getTotalShards());
    final VariationType variationType = flagConfig.getVariationType();
    if (variationType != null) {
      jgen.writeStringField("variationType", variationType.value);
    }
    final Map<String, Variation> variations = flagConfig.getVariations();
    if (variations != null) {
      jgen.writeFieldName("variations");
      jgen.writeStartObject();
      for (Map.Entry<String, Variation> entry : variations.entrySet()) {
        jgen.writeFieldName(entry.getKey());
        serializeVariation(entry.getValue(), jgen);
      }
      jgen.writeEndObject();
    }
    final List<Allocation> allocations = flagConfig.getAllocations();
    if (allocations != null) {
      jgen.writeFieldName("allocations");
      jgen.writeStartArray();
      for (Allocation allocation : allocations) {
        serializeAllocation(allocation, jgen, provider);
      }
      jgen.writeEndArray();
    }
    jgen.writeEndObject();
  }

  private void serializeVariation(Variation variation, JsonGenerator jgen)
      throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("key", variation.getKey());
    jgen.writeObjectField("value", variation.getValue());
    jgen.writeEndObject();
  }

  private void serializeAllocation(Allocation allocation, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("key", allocation.getKey());
    final Set<TargetingRule> rules = allocation.getRules();
    if (rules != null) {
      jgen.writeFieldName("rules");
      jgen.writeStartArray();
      for (TargetingRule rule : rules) {
        serializeTargetingRule(rule, jgen, provider);
      }
      jgen.writeEndArray();
    }
    final Date startAt = allocation.getStartAt();
    if (startAt != null) {
      jgen.writeStringField("startAt", getISODate(startAt));
    }
    final Date endAt = allocation.getEndAt();
    if (endAt != null) {
      jgen.writeStringField("endAt", getISODate(endAt));
    }
    final List<Split> splits = allocation.getSplits();
    if (splits != null) {
      jgen.writeFieldName("splits");
      jgen.writeStartArray();
      for (Split split : splits) {
        serializeSplit(split, jgen);
      }
      jgen.writeEndArray();
    }
    jgen.writeBooleanField("doLog", allocation.doLog());

    jgen.writeEndObject();
  }

  private void serializeTargetingRule(TargetingRule rule, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {
    jgen.writeStartObject();
    final Set<TargetingCondition> conditions = rule.getConditions();
    if (conditions != null) {
      jgen.writeFieldName("conditions");
      jgen.writeStartArray();
      for (TargetingCondition condition : conditions) {
        jgen.writeStartObject();
        jgen.writeStringField("attribute", condition.getAttribute());
        final OperatorType operator = condition.getOperator();
        if (operator != null) {
          jgen.writeStringField("operator", operator.value);
        }
        final EppoValue value = condition.getValue();
        if (value != null) {
          jgen.writeFieldName("value");
          eppoValueSerializer.serialize(value, jgen, provider);
        }

        jgen.writeEndObject();
      }
      jgen.writeEndArray();
    }
    jgen.writeEndObject();
  }

  private void serializeSplit(Split split, JsonGenerator jgen)
      throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("variationKey", split.getVariationKey());
    final Set<Shard> shards = split.getShards();
    if (shards != null) {
      jgen.writeFieldName("shards");
      jgen.writeStartArray();
      for (Shard shard : shards) {
        serializeShard(shard, jgen);
      }
      jgen.writeEndArray();
    }
    Map<String, String> extraLogging = split.getExtraLogging();
    if (extraLogging != null) {
      jgen.writeFieldName("extraLogging");
      jgen.writeStartObject();
      for (Map.Entry<String, String> extraLog : extraLogging.entrySet()) {
        jgen.writeStringField(extraLog.getKey(), extraLog.getValue());
      }
      jgen.writeEndObject();
    }
    jgen.writeEndObject();
  }

  private void serializeShard(Shard shard, JsonGenerator jgen)
      throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("salt", shard.getSalt());
    final Set<ShardRange> ranges = shard.getRanges();
    if (ranges != null) {
      jgen.writeFieldName("ranges");
      jgen.writeStartArray();
      for (ShardRange range : ranges) {
        jgen.writeStartObject();
        jgen.writeNumberField("start", range.getStart());
        jgen.writeNumberField("end", range.getEnd());
        jgen.writeEndObject();
      }
      jgen.writeEndArray();
    }
    jgen.writeEndObject();
  }

  private void serializeBanditReference(BanditReference banditReference, JsonGenerator jgen)
      throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("modelVersion", banditReference.getModelVersion());
    List<BanditFlagVariation> flagVariations = banditReference.getFlagVariations();
    if (flagVariations != null) {
      jgen.writeFieldName("flagVariations");
      jgen.writeStartArray();
      for (BanditFlagVariation flagVariation : flagVariations) {
        jgen.writeStartObject();
        jgen.writeStringField("key", flagVariation.getBanditKey());
        jgen.writeStringField("flagKey", flagVariation.getFlagKey());
        jgen.writeStringField("allocationKey", flagVariation.getAllocationKey());
        jgen.writeStringField("variationKey", flagVariation.getVariationKey());
        jgen.writeStringField("variationValue", flagVariation.getVariationValue());
        jgen.writeEndObject();
      }
      jgen.writeEndArray();
    }
    jgen.writeEndObject();
  }
}

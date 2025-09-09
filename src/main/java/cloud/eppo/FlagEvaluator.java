package cloud.eppo;

import static cloud.eppo.Utils.base64Decode;
import static cloud.eppo.Utils.getShard;
import static cloud.eppo.Utils.notNullBase64Decode;
import static cloud.eppo.Utils.throwIfEmptyOrNull;
import static cloud.eppo.Utils.throwIfNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import cloud.eppo.api.Attributes;
import cloud.eppo.api.EppoValue;
import cloud.eppo.model.ShardRange;
import cloud.eppo.ufc.dto.Allocation;
import cloud.eppo.ufc.dto.FlagConfig;
import cloud.eppo.ufc.dto.Shard;
import cloud.eppo.ufc.dto.Split;
import cloud.eppo.ufc.dto.Variation;
import cloud.eppo.ufc.dto.VariationType;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FlagEvaluator {

  @NotNull
  public static FlagEvaluationResult evaluateFlag(
      @NotNull FlagConfig flag,
      @NotNull String flagKey,
      @NotNull String subjectKey,
      @NotNull Attributes subjectAttributes,
      boolean isConfigObfuscated) {
    throwIfNull(flag, "flag must not be null");
    throwIfNull(flagKey, "flagKey must not be null");
    throwIfNull(subjectKey, "subjectKey must not be null");
    throwIfNull(subjectAttributes, "subjectAttributes must not be null");

    @NotNull final Date now = new Date();

    @Nullable Variation variation = null;
    @Nullable String allocationKey = null;
    @NotNull Map<String, String> extraLogging = new HashMap<>();
    boolean doLog = false;

    // If flag is disabled; use an empty list of allocations so that the empty result is returned
    // Note: this is a safety check; disabled flags should be filtered upstream
    @NotNull final List<Allocation> allocationsToConsider =
        flag.isEnabled() && flag.getAllocations() != null
            ? flag.getAllocations()
            : new LinkedList<>();

    for (Allocation allocation : allocationsToConsider) {
      if (allocation.getStartAt() != null && allocation.getStartAt().after(now)) {
        // Allocation not yet active
        continue;
      }
      if (allocation.getEndAt() != null && allocation.getEndAt().before(now)) {
        // Allocation no longer active
        continue;
      }

      // For convenience, we will automatically include the subject key as the "id" attribute if
      // none is provided
      @NotNull final Attributes subjectAttributesToEvaluate = new Attributes(subjectAttributes);
      if (!subjectAttributesToEvaluate.containsKey("id")) {
        subjectAttributesToEvaluate.put("id", subjectKey);
      }

      if (allocation.getRules() != null
          && !allocation.getRules().isEmpty()
          && RuleEvaluator.findMatchingRule(
                  subjectAttributesToEvaluate, allocation.getRules(), isConfigObfuscated)
              == null) {
        // Rules are defined, but none match
        continue;
      }

      // This allocation has matched; find variation
      for (@NotNull final Split split : allocation.getSplits()) {
        if (allShardsMatch(split, subjectKey, flag.getTotalShards(), isConfigObfuscated)) {
          // Variation and extra logging is determined by the relevant split
          variation = flag.getVariations().get(split.getVariationKey());
          if (variation == null) {
            throw new RuntimeException("Unknown split variation key: " + split.getVariationKey());
          }
          extraLogging = split.getExtraLogging();
          break;
        }
      }

      if (variation != null) {
        // We only evaluate the first relevant allocation
        allocationKey = allocation.getKey();
        // doLog is determined by the allocation
        doLog = allocation.doLog();
        break;
      }
    }

    if (isConfigObfuscated) {
      // Need to unobfuscate for the returned evaluation result
      if (allocationKey != null) {
        allocationKey = notNullBase64Decode(allocationKey);
      }
      if (variation != null) {
        @NotNull final String key = notNullBase64Decode(variation.getKey());
        @NotNull final EppoValue decodedValue;
        if (variation.getValue().isNull()) {
          decodedValue = EppoValue.nullValue();
        } else {
          @NotNull final String stringValue = notNullBase64Decode(variation.getValue().stringValue());
          @Nullable final VariationType variationType = flag.getVariationType();
          if (variationType == null) {
            throw new UnsupportedOperationException(
                    "Unexpected variation type for decoding obfuscated variation: "
                            + variationType);
          } else {
            switch (variationType) {
              case BOOLEAN:
                decodedValue = EppoValue.valueOf("true".equals(stringValue));
                break;
              case INTEGER:
              case NUMERIC:
                decodedValue = EppoValue.valueOf(Double.parseDouble(stringValue));
                break;
              case STRING:
              case JSON:
                decodedValue = EppoValue.valueOf(stringValue);
                break;
              default:
                throw new UnsupportedOperationException(
                        "Unexpected variation type for decoding obfuscated variation: "
                                + variationType);
            }
          }
        }
        variation = new Variation(key, decodedValue);
      }

      // Deobfuscate extraLogging if present
      if (extraLogging != null && !extraLogging.isEmpty()) {
        @NotNull final Map<String, String> deobfuscatedExtraLogging = new HashMap<>();
        for (@NotNull final Map.Entry<String, String> entry : extraLogging.entrySet()) {
          try {
            @Nullable final String deobfuscatedKey = base64Decode(entry.getKey());
            @Nullable final String deobfuscatedValue = base64Decode(entry.getValue());
            deobfuscatedExtraLogging.put(deobfuscatedKey, deobfuscatedValue);
          } catch (Exception e) {
            // If deobfuscation fails, keep the original key-value pair
            deobfuscatedExtraLogging.put(entry.getKey(), entry.getValue());
          }
        }
        extraLogging = deobfuscatedExtraLogging;
      }
    }

    @Nullable final FlagEvaluationAllocationKeyAndVariation allocationKeyAndVariation;
    if (allocationKey != null && variation != null) {
      // if allocationKey != null then variation is also != null
      allocationKeyAndVariation = new FlagEvaluationAllocationKeyAndVariation(allocationKey, variation);
    } else {
      allocationKeyAndVariation = null;
    }

    return new FlagEvaluationResult(
        flag, flagKey, subjectKey, subjectAttributes, allocationKeyAndVariation, extraLogging, doLog);
  }

  private static boolean allShardsMatch(
      @NotNull Split split, @NotNull String subjectKey, int totalShards, boolean isObfuscated) {
    if (split.getShards() == null || split.getShards().isEmpty()) {
      // Default to matching if no explicit shards
      return true;
    }

    for (@NotNull final Shard shard : split.getShards()) {
      if (!matchesShard(shard, subjectKey, totalShards, isObfuscated)) {
        return false;
      }
    }

    // If here, matchesShard() was true for each shard
    return true;
  }

  private static boolean matchesShard(
      @NotNull Shard shard, @NotNull String subjectKey, int totalShards, boolean isObfuscated) {
    @NotNull final String salt;
    if (isObfuscated) {
      salt = notNullBase64Decode(shard.getSalt());
    } else {
      salt = shard.getSalt();
    }
    @NotNull final String hashKey = salt + "-" + subjectKey;
    final int assignedShard = getShard(hashKey, totalShards);
    for (@NotNull final ShardRange range : shard.getRanges()) {
      if (assignedShard >= range.getStart() && assignedShard < range.getEnd()) {
        return true;
      }
    }

    // If here, the shard was not in any of the shard's ranges
    return false;
  }
}

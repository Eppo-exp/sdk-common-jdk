package cloud.eppo;

import static cloud.eppo.Utils.base64Decode;
import static cloud.eppo.Utils.getShard;

import cloud.eppo.api.Attributes;
import cloud.eppo.api.EppoValue;
import cloud.eppo.model.ShardRange;
import cloud.eppo.ufc.dto.Allocation;
import cloud.eppo.ufc.dto.FlagConfig;
import cloud.eppo.ufc.dto.Shard;
import cloud.eppo.ufc.dto.Split;
import cloud.eppo.ufc.dto.Variation;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FlagEvaluator {

  public static FlagEvaluationResult evaluateFlag(
      FlagConfig flag,
      String flagKey,
      String subjectKey,
      Attributes subjectAttributes,
      boolean isConfigObfuscated) {
    Date now = new Date();

    Variation variation = null;
    String allocationKey = null;
    Map<String, String> extraLogging = new HashMap<>();
    boolean doLog = false;

    // If flag is disabled; use an empty list of allocations so that the empty result is returned
    // Note: this is a safety check; disabled flags should be filtered upstream
    List<Allocation> allocationsToConsider =
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
      Attributes subjectAttributesToEvaluate = new Attributes(subjectAttributes);
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
      for (Split split : allocation.getSplits()) {
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
      allocationKey = base64Decode(allocationKey);
      if (variation != null) {
        String key = base64Decode(variation.getKey());
        EppoValue decodedValue = EppoValue.nullValue();
        if (!variation.getValue().isNull()) {
          String stringValue = base64Decode(variation.getValue().stringValue());
          switch (flag.getVariationType()) {
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
                      + flag.getVariationType());
          }
        }
        variation = new Variation(key, decodedValue);
      }
    }

    return new FlagEvaluationResult(
        flagKey, subjectKey, subjectAttributes, allocationKey, variation, extraLogging, doLog);
  }

  private static boolean allShardsMatch(
      Split split, String subjectKey, int totalShards, boolean isObfuscated) {
    if (split.getShards() == null || split.getShards().isEmpty()) {
      // Default to matching if no explicit shards
      return true;
    }

    for (Shard shard : split.getShards()) {
      if (!matchesShard(shard, subjectKey, totalShards, isObfuscated)) {
        return false;
      }
    }

    // If here, matchesShard() was true for each shard
    return true;
  }

  private static boolean matchesShard(
      Shard shard, String subjectKey, int totalShards, boolean isObfuscated) {
    String salt = shard.getSalt();
    if (isObfuscated) {
      salt = base64Decode(salt);
    }
    String hashKey = salt + "-" + subjectKey;
    int assignedShard = getShard(hashKey, totalShards);
    for (ShardRange range : shard.getRanges()) {
      if (assignedShard >= range.getStart() && assignedShard < range.getEnd()) {
        return true;
      }
    }

    // If here, the shard was not in any of the shard's ranges
    return false;
  }
}

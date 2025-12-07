package cloud.eppo;

import static cloud.eppo.Utils.base64Decode;
import static cloud.eppo.Utils.getMD5Hex;
import static cloud.eppo.Utils.getShard;

import cloud.eppo.api.AllocationDetails;
import cloud.eppo.api.AllocationEvaluationCode;
import cloud.eppo.api.Attributes;
import cloud.eppo.api.EppoValue;
import cloud.eppo.api.FlagEvaluationCode;
import cloud.eppo.api.MatchedRule;
import cloud.eppo.api.RuleCondition;
import cloud.eppo.model.ShardRange;
import cloud.eppo.ufc.dto.Allocation;
import cloud.eppo.ufc.dto.FlagConfig;
import cloud.eppo.ufc.dto.OperatorType;
import cloud.eppo.ufc.dto.Shard;
import cloud.eppo.ufc.dto.Split;
import cloud.eppo.ufc.dto.TargetingRule;
import cloud.eppo.ufc.dto.Variation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FlagEvaluator {

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

  /**
   * Evaluates a flag and returns detailed evaluation information including allocation statuses,
   * matched rules, and evaluation codes. This is useful for debugging and understanding why a
   * particular variation was assigned.
   */
  public static DetailedFlagEvaluationResult evaluateFlagWithDetails(
      FlagConfig flag,
      String flagKey,
      String subjectKey,
      Attributes subjectAttributes,
      boolean isConfigObfuscated) {
    Date now = new Date();

    DetailedFlagEvaluationResult.Builder builder =
        new DetailedFlagEvaluationResult.Builder()
            .flagKey(flagKey)
            .subjectKey(subjectKey)
            .subjectAttributes(subjectAttributes)
            .extraLogging(new HashMap<>());

    // Handle disabled flag
    if (!flag.isEnabled()) {
      builder
          .doLog(false)
          .flagEvaluationCode(FlagEvaluationCode.FLAG_UNRECOGNIZED_OR_DISABLED)
          .flagEvaluationDescription("Unrecognized or disabled flag: " + flagKey);

      // All allocations are unevaluated for disabled flags
      if (flag.getAllocations() != null) {
        for (int i = 0; i < flag.getAllocations().size(); i++) {
          Allocation allocation = flag.getAllocations().get(i);
          String allocationKey =
              isConfigObfuscated ? base64Decode(allocation.getKey()) : allocation.getKey();
          builder.addUnevaluatedAllocation(
              new AllocationDetails(allocationKey, AllocationEvaluationCode.UNEVALUATED, i + 1));
        }
      }

      return builder.build();
    }

    List<Allocation> allocationsToConsider =
        flag.getAllocations() != null ? flag.getAllocations() : new LinkedList<>();

    int allocationPosition = 0;
    boolean foundMatch = false;

    for (Allocation allocation : allocationsToConsider) {
      allocationPosition++;
      String allocationKey = allocation.getKey();
      String deobfuscatedAllocationKey =
          isConfigObfuscated ? base64Decode(allocationKey) : allocationKey;

      // Check if allocation is time-bound and not yet active
      if (allocation.getStartAt() != null && allocation.getStartAt().after(now)) {
        builder.addUnmatchedAllocation(
            new AllocationDetails(
                deobfuscatedAllocationKey,
                AllocationEvaluationCode.BEFORE_START_TIME,
                allocationPosition));
        continue;
      }

      // Check if allocation is time-bound and no longer active
      if (allocation.getEndAt() != null && allocation.getEndAt().before(now)) {
        builder.addUnmatchedAllocation(
            new AllocationDetails(
                deobfuscatedAllocationKey,
                AllocationEvaluationCode.AFTER_END_TIME,
                allocationPosition));
        continue;
      }

      // For convenience, automatically include subject key as "id" attribute if not provided
      Attributes subjectAttributesToEvaluate = new Attributes(subjectAttributes);
      if (!subjectAttributesToEvaluate.containsKey("id")) {
        subjectAttributesToEvaluate.put("id", subjectKey);
      }

      // Check rules
      TargetingRule matchedTargetingRule = null;
      if (allocation.getRules() != null && !allocation.getRules().isEmpty()) {
        matchedTargetingRule =
            RuleEvaluator.findMatchingRule(
                subjectAttributesToEvaluate, allocation.getRules(), isConfigObfuscated);

        if (matchedTargetingRule == null) {
          // Rules are defined but none match
          builder.addUnmatchedAllocation(
              new AllocationDetails(
                  deobfuscatedAllocationKey,
                  AllocationEvaluationCode.FAILING_RULE,
                  allocationPosition));
          continue;
        }
      }

      // This allocation has matched rules; find variation in splits
      Variation variation = null;
      Map<String, String> extraLogging = new HashMap<>();
      Split matchedSplit = null;

      for (Split split : allocation.getSplits()) {
        if (allShardsMatch(split, subjectKey, flag.getTotalShards(), isConfigObfuscated)) {
          variation = flag.getVariations().get(split.getVariationKey());
          if (variation == null) {
            throw new RuntimeException("Unknown split variation key: " + split.getVariationKey());
          }
          extraLogging = split.getExtraLogging();
          matchedSplit = split;
          break;
        }
      }

      if (variation == null) {
        // Rules matched but subject doesn't fall in traffic split
        builder.addUnmatchedAllocation(
            new AllocationDetails(
                deobfuscatedAllocationKey,
                AllocationEvaluationCode.TRAFFIC_EXPOSURE_MISS,
                allocationPosition));
        continue;
      }

      foundMatch = true;

      // Deobfuscate if needed
      if (isConfigObfuscated) {
        allocationKey = deobfuscatedAllocationKey;
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

        // Deobfuscate extraLogging if present
        if (extraLogging != null && !extraLogging.isEmpty()) {
          Map<String, String> deobfuscatedExtraLogging = new HashMap<>();
          for (Map.Entry<String, String> entry : extraLogging.entrySet()) {
            try {
              String deobfuscatedKey = base64Decode(entry.getKey());
              String deobfuscatedValue = base64Decode(entry.getValue());
              deobfuscatedExtraLogging.put(deobfuscatedKey, deobfuscatedValue);
            } catch (Exception e) {
              deobfuscatedExtraLogging.put(entry.getKey(), entry.getValue());
            }
          }
          extraLogging = deobfuscatedExtraLogging;
        }
      }

      // Build matched rule details if applicable
      MatchedRule matchedRule = null;
      if (matchedTargetingRule != null) {
        // Build reverse lookup map for deobfuscating ONE_OF values
        Map<String, String> md5ToOriginalValue = null;
        if (isConfigObfuscated) {
          md5ToOriginalValue = new HashMap<>();
          for (Map.Entry<String, EppoValue> entry : subjectAttributesToEvaluate.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isNull()) {
              // Hash the attribute value (as string) to create reverse lookup
              String valueAsString = castAttributeValueToString(entry.getValue());
              if (valueAsString != null) {
                md5ToOriginalValue.put(getMD5Hex(valueAsString), valueAsString);
              }
            }
          }
        }

        final Map<String, String> finalMd5ToOriginalValue = md5ToOriginalValue;
        Set<RuleCondition> conditions =
            matchedTargetingRule.getConditions().stream()
                .map(
                    tc -> {
                      // Deobfuscate attribute name if config is obfuscated
                      String attribute = tc.getAttribute();
                      if (isConfigObfuscated) {
                        // Find the original attribute name by matching the MD5 hash
                        for (Map.Entry<String, EppoValue> entry :
                            subjectAttributesToEvaluate.entrySet()) {
                          if (getMD5Hex(entry.getKey()).equals(attribute)) {
                            attribute = entry.getKey();
                            break;
                          }
                        }
                      }

                      // Deobfuscate the condition value if needed
                      EppoValue value = tc.getValue();
                      if (isConfigObfuscated && value != null) {
                        Object deobfuscatedValue =
                            deobfuscateConditionValue(
                                tc.getValue(), tc.getOperator(), finalMd5ToOriginalValue);
                        // Convert deobfuscated Object back to EppoValue
                        value = objectToEppoValue(deobfuscatedValue);
                      }

                      return new RuleCondition(attribute, tc.getOperator().value, value);
                    })
                .collect(Collectors.toSet());
        matchedRule = new MatchedRule(conditions);
      }

      // Determine evaluation description
      String description;
      if (matchedRule != null) {
        // Check if we need to include traffic assignment details
        // Include traffic details if there are multiple splits OR multiple shards
        boolean hasMultipleSplits = allocation.getSplits().size() > 1;
        boolean hasMultipleShards =
            matchedSplit.getShards() != null && matchedSplit.getShards().size() > 1;

        if (hasMultipleSplits || hasMultipleShards) {
          description =
              String.format(
                  "Supplied attributes match rules defined in allocation \"%s\" and %s belongs to the range of traffic assigned to \"%s\".",
                  allocationKey, subjectKey, variation.getKey());
        } else {
          description =
              String.format(
                  "Supplied attributes match rules defined in allocation \"%s\".", allocationKey);
        }
      } else {
        description =
            String.format(
                "%s belongs to the range of traffic assigned to \"%s\" defined in allocation \"%s\".",
                subjectKey, variation.getKey(), allocationKey);
      }

      // TODO check type

      builder
          .allocationKey(allocationKey)
          .variation(variation)
          .extraLogging(extraLogging)
          .doLog(allocation.doLog())
          .flagEvaluationCode(FlagEvaluationCode.MATCH)
          .flagEvaluationDescription(description)
          .matchedRule(matchedRule)
          .matchedAllocation(
              new AllocationDetails(
                  allocationKey, AllocationEvaluationCode.MATCH, allocationPosition));

      // Mark remaining allocations as unevaluated
      for (int i = allocationPosition; i < allocationsToConsider.size(); i++) {
        Allocation unevaluatedAllocation = allocationsToConsider.get(i);
        String unevaluatedKey =
            isConfigObfuscated
                ? base64Decode(unevaluatedAllocation.getKey())
                : unevaluatedAllocation.getKey();
        builder.addUnevaluatedAllocation(
            new AllocationDetails(unevaluatedKey, AllocationEvaluationCode.UNEVALUATED, i + 2));
      }

      break;
    }

    // If no match was found, return default with appropriate code
    if (!foundMatch) {
      builder
          .doLog(false)
          .flagEvaluationCode(FlagEvaluationCode.DEFAULT_ALLOCATION_NULL)
          .flagEvaluationDescription(
              "No allocations matched. Falling back to \"Default Allocation\", serving NULL");
    }

    return builder.build();
  }

  private static Object getEppoValueAsObject(EppoValue value) {
    if (value.isNull()) {
      return null;
    } else if (value.isBoolean()) {
      return value.booleanValue();
    } else if (value.isNumeric()) {
      return value.doubleValue();
    } else if (value.isString()) {
      return value.stringValue();
    } else if (value.isStringArray()) {
      return value.stringArrayValue();
    }
    return null;
  }

  /**
   * Deobfuscates a condition value based on the operator type. Different operators obfuscate values
   * differently: - IS_NULL: MD5 hash of "true" or "false" - Inequality operators (GTE, GT, LTE,
   * LT): base64 encoded numbers or semver strings - MATCHES, NOT_MATCHES: base64 encoded regex
   * patterns - ONE_OF, NOT_ONE_OF: array of MD5 hashes that need to be reverse-looked up
   */
  private static Object deobfuscateConditionValue(
      EppoValue value, OperatorType operator, Map<String, String> md5ToOriginalValue) {
    if (value.isNull()) {
      return null;
    }

    switch (operator) {
      case IS_NULL:
        // Check if it's MD5 of "true" or "false"
        if (value.isString()) {
          String hash = value.stringValue();
          if (getMD5Hex("true").equals(hash)) {
            return true;
          } else if (getMD5Hex("false").equals(hash)) {
            return false;
          }
        }
        return value.booleanValue();

      case GREATER_THAN_OR_EQUAL_TO:
      case GREATER_THAN:
      case LESS_THAN_OR_EQUAL_TO:
      case LESS_THAN:
        // Decode base64 encoded numeric or semver values
        if (value.isString()) {
          try {
            String decoded = base64Decode(value.stringValue());
            // Try to parse as number first
            try {
              return Double.parseDouble(decoded);
            } catch (NumberFormatException e) {
              // Return as string (likely a semver)
              return decoded;
            }
          } catch (Exception e) {
            // If decode fails, return original
            return value.stringValue();
          }
        }
        return getEppoValueAsObject(value);

      case MATCHES:
      case NOT_MATCHES:
        // Decode base64 encoded regex patterns
        if (value.isString()) {
          try {
            return base64Decode(value.stringValue());
          } catch (Exception e) {
            return value.stringValue();
          }
        }
        return value.stringValue();

      case ONE_OF:
      case NOT_ONE_OF:
        // Array values are MD5 hashes - try to reverse them using the subject attributes
        if (value.isStringArray() && md5ToOriginalValue != null) {
          List<String> deobfuscatedValues = new ArrayList<>();
          for (String hash : value.stringArrayValue()) {
            String originalValue = md5ToOriginalValue.get(hash);
            if (originalValue != null) {
              deobfuscatedValues.add(originalValue);
            } else {
              // Keep the hash if we can't reverse it
              deobfuscatedValues.add(hash);
            }
          }
          return deobfuscatedValues;
        }
        return getEppoValueAsObject(value);

      default:
        return getEppoValueAsObject(value);
    }
  }

  /**
   * Casts an EppoValue to a string representation for use in hash lookups. Uses the same logic as
   * RuleEvaluator.castAttributeForListComparison()
   */
  private static String castAttributeValueToString(EppoValue attributeValue) {
    if (attributeValue.isBoolean()) {
      return Boolean.valueOf(attributeValue.booleanValue()).toString();
    } else if (attributeValue.isNumeric()) {
      double doubleValue = attributeValue.doubleValue();
      int intValue = (int) attributeValue.doubleValue();
      return doubleValue == intValue ? String.valueOf(intValue) : String.valueOf(doubleValue);
    } else if (attributeValue.isString()) {
      return attributeValue.stringValue();
    } else if (attributeValue.isStringArray()) {
      return Collections.singletonList(attributeValue.stringArrayValue()).toString();
    } else if (attributeValue.isNull()) {
      return "";
    } else {
      return null;
    }
  }

  /** Converts an Object back to EppoValue after deobfuscation. */
  private static EppoValue objectToEppoValue(Object value) {
    if (value == null) {
      return EppoValue.nullValue();
    } else if (value instanceof Boolean) {
      return EppoValue.valueOf((Boolean) value);
    } else if (value instanceof Double) {
      return EppoValue.valueOf((Double) value);
    } else if (value instanceof Integer) {
      return EppoValue.valueOf(((Integer) value).doubleValue());
    } else if (value instanceof String) {
      return EppoValue.valueOf((String) value);
    } else if (value instanceof List) {
      @SuppressWarnings("unchecked")
      List<String> list = (List<String>) value;
      return EppoValue.valueOf(list);
    } else {
      return EppoValue.valueOf(value.toString());
    }
  }
}

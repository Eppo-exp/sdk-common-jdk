package cloud.eppo;

import static cloud.eppo.Utils.base64Decode;
import static cloud.eppo.Utils.getMD5Hex;

import cloud.eppo.api.Attributes;
import cloud.eppo.api.EppoValue;
import cloud.eppo.ufc.dto.OperatorType;
import cloud.eppo.ufc.dto.TargetingCondition;
import cloud.eppo.ufc.dto.TargetingRule;
import com.github.zafarkhaja.semver.Version;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class RuleEvaluator {

  public static TargetingRule findMatchingRule(
      Attributes subjectAttributes, Set<TargetingRule> rules, boolean isObfuscated) {
    for (TargetingRule rule : rules) {
      if (allConditionsMatch(subjectAttributes, rule.getConditions(), isObfuscated)) {
        return rule;
      }
    }
    return null;
  }

  private static boolean allConditionsMatch(
      Attributes subjectAttributes, Set<TargetingCondition> conditions, boolean isObfuscated) {
    for (TargetingCondition condition : conditions) {
      if (!evaluateCondition(subjectAttributes, condition, isObfuscated)) {
        return false;
      }
    }
    return true;
  }

  private static boolean evaluateCondition(
      Attributes subjectAttributes, TargetingCondition condition, boolean isObfuscated) {
    EppoValue conditionValue = condition.getValue();
    String attributeKey = condition.getAttribute();
    EppoValue attributeValue = null;
    if (isObfuscated) {
      // attribute names are hashed
      for (Map.Entry<String, EppoValue> entry : subjectAttributes.entrySet()) {
        if (getMD5Hex(entry.getKey()).equals(attributeKey)) {
          attributeValue = entry.getValue();
          break;
        }
      }
    } else {
      attributeValue = subjectAttributes.get(attributeKey);
    }

    // First we do any NULL check
    boolean attributeValueIsNull = attributeValue == null || attributeValue.isNull();
    OperatorType operator = condition.getOperator();
    if (operator == OperatorType.IS_NULL) {
      boolean expectNull =
          isObfuscated
              ? getMD5Hex("true").equals(conditionValue.stringValue())
              : conditionValue.booleanValue();
      return expectNull && attributeValueIsNull || !expectNull && !attributeValueIsNull;
    } else if (attributeValueIsNull) {
      // Any check other than IS NULL should fail if the attribute value is null
      return false;
    }

    if (operator.isInequalityComparison()) {
      Double conditionNumber = null;
      if (isObfuscated && conditionValue.isString()) {
        // it may be an encoded number
        try {
          conditionNumber = Double.parseDouble(base64Decode(conditionValue.stringValue()));
        } catch (Exception e) {
          // not a number
        }
      } else if (conditionValue.isNumeric()) {
        conditionNumber = conditionValue.doubleValue();
      }

      boolean numericComparison = attributeValue.isNumeric() && conditionNumber != null;

      // Android API version 21 does not have access to the java.util.Optional class.
      // Version.tryParse returns an Optional<Version> would be ideal.
      // Instead, use Version.parse which throws an exception if the string is not a valid SemVer.
      // We front-load the parsing here so many evaluation of gte, gt, lte, lt operations
      // more straight-forward.
      Version valueSemVer = null;
      Version conditionSemVer = null;
      try {
        valueSemVer = Version.parse(attributeValue.stringValue());
        String conditionSemVerString = condition.getValue().stringValue();
        if (isObfuscated) {
          conditionSemVerString = base64Decode(conditionSemVerString);
        }
        conditionSemVer = Version.parse(conditionSemVerString);
      } catch (Exception e) {
        // no-op
      }

      // Performing this check satisfies the compiler that the possibly
      // null value can be safely accessed later.
      boolean semVerComparison = valueSemVer != null && conditionSemVer != null;

      switch (operator) {
        case GREATER_THAN_OR_EQUAL_TO:
          if (numericComparison) {
            return attributeValue.doubleValue() >= conditionNumber;
          }

          if (semVerComparison) {
            return valueSemVer.isHigherThanOrEquivalentTo(conditionSemVer);
          }

          return false;
        case GREATER_THAN:
          if (numericComparison) {
            return attributeValue.doubleValue() > conditionNumber;
          }

          if (semVerComparison) {
            return valueSemVer.isHigherThan(conditionSemVer);
          }

          return false;
        case LESS_THAN_OR_EQUAL_TO:
          if (numericComparison) {
            return attributeValue.doubleValue() <= conditionNumber;
          }

          if (semVerComparison) {
            return valueSemVer.isLowerThanOrEquivalentTo(conditionSemVer);
          }

          return false;
        case LESS_THAN:
          if (numericComparison) {
            return attributeValue.doubleValue() < conditionNumber;
          }

          if (semVerComparison) {
            return valueSemVer.isLowerThan(conditionSemVer);
          }

          return false;
        default:
          throw new IllegalStateException("Unexpected inequality operator: " + operator);
      }
    }

    if (operator.isListComparison()) {
      boolean expectMatch = operator == OperatorType.ONE_OF;
      boolean matchFound = false;
      for (String arrayString : conditionValue.stringArrayValue()) {
        String comparisonString = castAttributeForListComparison(attributeValue);
        if (isObfuscated) {
          // List comparisons use hashes for checking exact match
          comparisonString = getMD5Hex(comparisonString);
        }
        if (arrayString.equals(comparisonString)) {
          matchFound = true;
          break;
        }
      }
      return expectMatch && matchFound || !expectMatch && !matchFound;
    }

    if (operator == OperatorType.MATCHES || operator == OperatorType.NOT_MATCHES) {
      // Regexes require decoding
      String patternString = condition.getValue().stringValue();
      if (isObfuscated) {
        patternString = base64Decode(patternString);
      }

      // Use find() to support partial matching
      Pattern pattern = Pattern.compile(patternString);
      boolean patternFound = pattern.matcher(attributeValue.toString()).find();
      return (operator == OperatorType.MATCHES) == patternFound;
    }

    throw new IllegalStateException("Unexpected rule operator: " + operator);
  }

  /**
   * IN and NOT IN checks are not strongly typed, as the user is only entering in strings Thus we
   * need to cast the attribute to a string before hashing and checking
   */
  private static String castAttributeForListComparison(EppoValue attributeValue) {
    if (attributeValue.isBoolean()) {
      return Boolean.valueOf(attributeValue.booleanValue()).toString();
    } else if (attributeValue.isNumeric()) {
      double doubleValue = attributeValue.doubleValue();
      int intValue = Double.valueOf(attributeValue.doubleValue()).intValue();
      return doubleValue == intValue ? String.valueOf(intValue) : String.valueOf(doubleValue);
    } else if (attributeValue.isString()) {
      return attributeValue.stringValue();
    } else if (attributeValue.isStringArray()) {
      return Collections.singletonList(attributeValue.stringArrayValue()).toString();
    } else if (attributeValue.isNull()) {
      return "";
    } else {
      throw new IllegalArgumentException(
          "Unknown EppoValue type for casting for list comparison: " + attributeValue);
    }
  }
}

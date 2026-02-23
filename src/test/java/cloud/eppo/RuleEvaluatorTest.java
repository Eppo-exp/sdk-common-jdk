package cloud.eppo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import cloud.eppo.api.Attributes;
import cloud.eppo.api.EppoValue;
import cloud.eppo.api.dto.OperatorType;
import cloud.eppo.api.dto.TargetingCondition;
import cloud.eppo.api.dto.TargetingRule;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class RuleEvaluatorTest {

  public TargetingRule createRule(Set<TargetingCondition> conditions) {
    return new TargetingRule.Default(conditions);
  }

  public Set<TargetingCondition> createNumericConditions() {
    Set<TargetingCondition> conditions = new HashSet<>();
    conditions.add(
        new TargetingCondition.Default(
            OperatorType.GREATER_THAN_OR_EQUAL_TO, "price", EppoValue.valueOf(10)));
    conditions.add(
        new TargetingCondition.Default(
            OperatorType.LESS_THAN_OR_EQUAL_TO, "price", EppoValue.valueOf(20)));
    return conditions;
  }

  public Set<TargetingCondition> createSemVerConditions() {
    Set<TargetingCondition> conditions = new HashSet<>();
    conditions.add(
        new TargetingCondition.Default(
            OperatorType.GREATER_THAN_OR_EQUAL_TO, "appVersion", EppoValue.valueOf("1.5.0")));
    conditions.add(
        new TargetingCondition.Default(
            OperatorType.LESS_THAN, "appVersion", EppoValue.valueOf("2.2.0")));
    return conditions;
  }

  public Set<TargetingCondition> createRegexConditions() {
    Set<TargetingCondition> conditions = new HashSet<>();
    conditions.add(
        new TargetingCondition.Default(
            OperatorType.MATCHES, "match", EppoValue.valueOf("example\\.(com|org)")));
    return conditions;
  }

  public Set<TargetingCondition> createOneOfConditionsWithStrings() {
    Set<TargetingCondition> conditions = new HashSet<>();
    List<String> values = Arrays.asList("value1", "value2");
    conditions.add(
        new TargetingCondition.Default(OperatorType.ONE_OF, "oneOf", EppoValue.valueOf(values)));
    return conditions;
  }

  public Set<TargetingCondition> createOneOfConditionsWithIntegers() {
    Set<TargetingCondition> conditions = new HashSet<>();
    List<String> values = Arrays.asList("1", "2");
    conditions.add(
        new TargetingCondition.Default(OperatorType.ONE_OF, "oneOf", EppoValue.valueOf(values)));
    return conditions;
  }

  public Set<TargetingCondition> createOneOfConditionsWithDoubles() {
    Set<TargetingCondition> conditions = new HashSet<>();
    List<String> values = Arrays.asList("1.5", "2.7");
    conditions.add(
        new TargetingCondition.Default(OperatorType.ONE_OF, "oneOf", EppoValue.valueOf(values)));
    return conditions;
  }

  public Set<TargetingCondition> createOneOfConditionsWithBoolean() {
    Set<TargetingCondition> conditions = new HashSet<>();
    List<String> values = Collections.singletonList("true");
    conditions.add(
        new TargetingCondition.Default(OperatorType.ONE_OF, "oneOf", EppoValue.valueOf(values)));
    return conditions;
  }

  public Set<TargetingCondition> createNotOneOfConditions() {
    Set<TargetingCondition> conditions = new HashSet<>();
    List<String> values = Arrays.asList("value1", "value2");
    conditions.add(
        new TargetingCondition.Default(
            OperatorType.NOT_ONE_OF, "oneOf", EppoValue.valueOf(values)));
    return conditions;
  }

  public void addNameToSubjectAttribute(Attributes subjectAttributes) {
    subjectAttributes.put("name", "test");
  }

  public void addPriceToSubjectAttribute(Attributes subjectAttributes) {
    subjectAttributes.put("price", "30");
  }

  @Test
  public void testMatchesAnyRuleWithEmptyConditions() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    final TargetingRule targetingRuleWithEmptyConditions = createRule(new HashSet<>());
    targetingRules.add(targetingRuleWithEmptyConditions);
    Attributes subjectAttributes = new Attributes();
    addNameToSubjectAttribute(subjectAttributes);

    assertEquals(
        targetingRuleWithEmptyConditions,
        RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWithEmptyRules() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    Attributes subjectAttributes = new Attributes();
    addNameToSubjectAttribute(subjectAttributes);

    assertNull(RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWhenNoRuleMatches() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createNumericConditions());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    addPriceToSubjectAttribute(subjectAttributes);

    assertNull(RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWhenRuleMatches() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createNumericConditions());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("price", 15);

    assertEquals(
        targetingRule, RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWhenRuleMatchesWithSemVer() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createSemVerConditions());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("appVersion", "1.15.5");

    assertEquals(
        targetingRule, RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWhenThrowInvalidSubjectAttribute() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createNumericConditions());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("price", EppoValue.valueOf("abcd"));

    assertNull(RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWithRegexCondition() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createRegexConditions());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("match", EppoValue.valueOf("test@example.com"));

    assertEquals(
        targetingRule, RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWithRegexConditionNotMatched() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createRegexConditions());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("match", EppoValue.valueOf("123"));

    assertNull(RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWithNotOneOfRule() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createNotOneOfConditions());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf("value3"));

    assertEquals(
        targetingRule, RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWithNotOneOfRuleNotPassed() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createNotOneOfConditions());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf("value1"));

    assertNull(RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWithOneOfRuleOnString() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createOneOfConditionsWithStrings());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf("value1"));

    assertEquals(
        targetingRule, RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWithOneOfRuleOnInteger() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createOneOfConditionsWithIntegers());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf(2));

    assertEquals(
        targetingRule, RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWithOneOfRuleOnDouble() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createOneOfConditionsWithDoubles());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf(1.5));

    assertEquals(
        targetingRule, RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }

  @Test
  public void testMatchesAnyRuleWithOneOfRuleOnBoolean() {
    Set<TargetingRule> targetingRules = new HashSet<>();
    TargetingRule targetingRule = createRule(createOneOfConditionsWithBoolean());
    targetingRules.add(targetingRule);

    Attributes subjectAttributes = new Attributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf(true));

    assertEquals(
        targetingRule, RuleEvaluator.findMatchingRule(subjectAttributes, targetingRules, false));
  }
}

package cloud.eppo.api;

import java.util.Set;

/** Details about a rule that matched during allocation evaluation. */
public class MatchedRule {
  private final Set<RuleCondition> conditions;

  public MatchedRule(Set<RuleCondition> conditions) {
    this.conditions = conditions;
  }

  public Set<RuleCondition> getConditions() {
    return conditions;
  }
}

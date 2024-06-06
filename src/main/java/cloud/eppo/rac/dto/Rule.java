package cloud.eppo.rac.dto;

import java.util.List;

/** Rule Class */
public class Rule {
  private final String allocationKey;
  private final List<Condition> conditions;

  public Rule(String allocationKey, List<Condition> conditions) {
    this.allocationKey = allocationKey;
    this.conditions = conditions;
  }

  public String getAllocationKey() {
    return allocationKey;
  }

  public List<Condition> getConditions() {
    return conditions;
  }
}

package cloud.eppo.rac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Rule Class */
public class Rule {
  private final String allocationKey;
  private final List<Condition> conditions;

  @JsonCreator
  public Rule(
      @JsonProperty("allocationKey") String allocationKey,
      @JsonProperty("conditions") List<Condition> conditions) {
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

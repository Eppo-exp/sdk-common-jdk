package cloud.eppo.rac.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Experiment Configuration Class */
public class ExperimentConfiguration {
  private final String name;
  private final boolean enabled;
  private final int subjectShards;
  private final Map<String, EppoValue> typedOverrides = new HashMap<>();
  private final Map<String, Allocation> allocations;
  private final List<Rule> rules;

  public ExperimentConfiguration(
      String name,
      boolean enabled,
      int subjectShards,
      Map<String, Allocation> allocations,
      List<Rule> rules) {
    this.name = name;
    this.enabled = enabled;
    this.subjectShards = subjectShards;
    this.allocations = allocations;
    this.rules = rules;
  }

  public Allocation getAllocation(String allocationKey) {
    return allocations.get(allocationKey);
  }

  public String getName() {
    return name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public int getSubjectShards() {
    return subjectShards;
  }

  public Map<String, EppoValue> getTypedOverrides() {
    return typedOverrides;
  }

  public List<Rule> getRules() {
    return rules;
  }

  public Map<String, Allocation> getAllocations() {
    return allocations;
  }
}

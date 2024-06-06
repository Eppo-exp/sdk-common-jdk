package cloud.eppo.rac.dto;

import java.util.Map;

/** Experiment Configuration Response Class */
public class ExperimentConfigurationResponse {
  private final Map<String, ExperimentConfiguration> flags;

  public ExperimentConfigurationResponse(Map<String, ExperimentConfiguration> flags) {
    this.flags = flags;
  }

  public Map<String, ExperimentConfiguration> getFlags() {
    return flags;
  }
}

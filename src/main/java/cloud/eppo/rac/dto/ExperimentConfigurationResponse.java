package cloud.eppo.rac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Experiment Configuration Response Class */
public class ExperimentConfigurationResponse {
  private final Map<String, ExperimentConfiguration> flags;

  @JsonCreator
  public ExperimentConfigurationResponse(
      @JsonProperty("flags") Map<String, ExperimentConfiguration> flags) {
    this.flags = flags;
  }

  public Map<String, ExperimentConfiguration> getFlags() {
    return flags;
  }
}

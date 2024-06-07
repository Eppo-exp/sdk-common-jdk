package cloud.eppo.rac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Allocation {
  private final double percentExposure;
  private final List<Variation> variations;

  @JsonCreator
  public Allocation(
      @JsonProperty("percentExposure") double percentExposure,
      @JsonProperty("allocationKey") List<Variation> variations) {
    this.percentExposure = percentExposure;
    this.variations = variations;
  }

  public double getPercentExposure() {
    return percentExposure;
  }

  public List<Variation> getVariations() {
    return variations;
  }
}

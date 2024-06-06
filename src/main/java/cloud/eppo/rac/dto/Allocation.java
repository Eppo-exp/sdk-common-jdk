package cloud.eppo.rac.dto;

import java.util.List;

public class Allocation {
  private final double percentExposure;
  private final List<Variation> variations;

  public Allocation(double percentExposure, List<Variation> variations) {
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

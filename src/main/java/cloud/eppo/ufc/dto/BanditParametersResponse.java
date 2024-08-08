package cloud.eppo.ufc.dto;

import java.util.HashMap;
import java.util.Map;

public class BanditParametersResponse {

  private final Map<String, BanditParameters> bandits;

  public BanditParametersResponse() {
    this.bandits = new HashMap<>();
  }

  public BanditParametersResponse(Map<String, BanditParameters> bandits) {
    this.bandits = bandits;
  }

  public Map<String, BanditParameters> getBandits() {
    return bandits;
  }
}

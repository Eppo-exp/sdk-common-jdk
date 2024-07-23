package cloud.eppo.ufc.dto;

import java.util.Map;

public class BanditParametersResponse {

  private Map<String, BanditParameters> bandits;

  public BanditParametersResponse() {}

  public Map<String, BanditParameters> getBandits() {
    return bandits;
  }
}

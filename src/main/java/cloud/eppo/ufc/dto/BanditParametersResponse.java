package cloud.eppo.ufc.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BanditParametersResponse {

  private final Map<String, BanditParameters> bandits;

  public BanditParametersResponse() {
    this.bandits = new HashMap<>();
  }

  public BanditParametersResponse(Map<String, BanditParameters> bandits) {
    this.bandits = bandits;
  }

  @Override
  public String toString() {
    return "BanditParametersResponse{" +
      "bandits=" + bandits +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BanditParametersResponse that = (BanditParametersResponse) o;
    return Objects.equals(bandits, that.bandits);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(bandits);
  }

  public Map<String, BanditParameters> getBandits() {
    return bandits;
  }
}

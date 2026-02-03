package cloud.eppo.api.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public interface BanditParametersResponse {
  Map<String, BanditParameters> getBandits();

  class Default implements BanditParametersResponse {
    private final Map<String, BanditParameters> bandits;

    public Default() {
      this.bandits = new HashMap<>();
    }

    public Default(Map<String, BanditParameters> bandits) {
      this.bandits = bandits;
    }

    @Override
    public String toString() {
      return "BanditParametersResponse{" + "bandits=" + bandits + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      BanditParametersResponse that = (BanditParametersResponse) o;
      return Objects.equals(bandits, that.getBandits());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(bandits);
    }

    @Override
    public Map<String, BanditParameters> getBandits() {
      return bandits;
    }
  }
}

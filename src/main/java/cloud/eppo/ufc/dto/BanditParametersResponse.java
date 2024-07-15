package cloud.eppo.ufc.dto;

import cloud.eppo.rac.deserializer.BanditsDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import java.util.Map;

public class BanditParametersResponse {
  @JsonProperty private Date updatedAt;

  @JsonDeserialize(using = BanditsDeserializer.class)
  private Map<String, BanditParameters> bandits;

  public BanditParametersResponse() {}

  public Map<String, BanditParameters> getBandits() {
    return bandits;
  }
}

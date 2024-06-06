package cloud.eppo.rac.dto;

import com.eppo.sdk.deserializer.BanditsDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import java.util.Map;
import lombok.Data;

@Data
public class BanditParametersResponse {
  private Date updatedAt;

  @JsonDeserialize(using = BanditsDeserializer.class)
  private Map<String, BanditParameters> bandits;
}

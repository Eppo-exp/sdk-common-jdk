package cloud.eppo.ufc.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlagConfigResponse {
  private final Map<String, FlagConfig> flags;
  private final Map<String, BanditReference> banditReferences;

  public FlagConfigResponse(Map<String, FlagConfig> flags, Map<String, BanditReference> banditReferences) {
    this.flags = flags;
    this.banditReferences = banditReferences;
  }

  public FlagConfigResponse() {
    this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
  }

  public Map<String, FlagConfig> getFlags() {
    return this.flags;
  }

  public Map<String, BanditReference> getBanditReferences() {
    return this.banditReferences;
  }
}

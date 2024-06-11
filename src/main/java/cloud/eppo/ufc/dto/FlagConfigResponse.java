package cloud.eppo.ufc.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlagConfigResponse {
  private final Map<String, FlagConfig> flags;

  public FlagConfigResponse(Map<String, FlagConfig> flags) {
    this.flags = flags;
  }

  public FlagConfigResponse() {
    this(new ConcurrentHashMap<>());
  }

  public Map<String, FlagConfig> getFlags() {
    return this.flags;
  }
}

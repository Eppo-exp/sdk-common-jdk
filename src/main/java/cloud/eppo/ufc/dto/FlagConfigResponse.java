package cloud.eppo.ufc.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlagConfigResponse {
  private final Map<String, FlagConfig> flags;
  private final Map<String, BanditReference> banditReferences;
  private final boolean forServer;

  public FlagConfigResponse(
      Map<String, FlagConfig> flags, Map<String, BanditReference> banditReferences) {
    this(flags, banditReferences, false);
  }

  public FlagConfigResponse(
      Map<String, FlagConfig> flags,
      Map<String, BanditReference> banditReferences,
      boolean isConfigObfuscated) {
    this.flags = flags;
    this.banditReferences = banditReferences;
    this.forServer = !isConfigObfuscated;
  }

  public FlagConfigResponse() {
    this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), false);
  }

  public Map<String, FlagConfig> getFlags() {
    return this.flags;
  }

  public Map<String, BanditReference> getBanditReferences() {
    return this.banditReferences;
  }

  public boolean isForServer() {
    return this.forServer;
  }
}

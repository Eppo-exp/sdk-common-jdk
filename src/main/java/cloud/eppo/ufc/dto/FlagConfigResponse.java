package cloud.eppo.ufc.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlagConfigResponse {
  private final Map<String, FlagConfig> flags;
  private final Map<String, BanditReference> banditReferences;
  private final Format format;

  public FlagConfigResponse(
      Map<String, FlagConfig> flags,
      Map<String, BanditReference> banditReferences,
      Format dataFormat) {
    this.flags = flags;
    this.banditReferences = banditReferences;
    format = dataFormat;
  }

  public FlagConfigResponse(
      Map<String, FlagConfig> flags, Map<String, BanditReference> banditReferences) {
    this(flags, banditReferences, Format.SERVER);
  }

  public FlagConfigResponse() {
    this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), Format.SERVER);
  }

  public Map<String, FlagConfig> getFlags() {
    return this.flags;
  }

  public Map<String, BanditReference> getBanditReferences() {
    return this.banditReferences;
  }

  public Format getFormat() {
    return format;
  }

  public enum Format {
    SERVER,
    CLIENT
  }
}

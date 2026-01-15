package cloud.eppo.ufc.dto;

import cloud.eppo.api.IFlagConfigResponse;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class FlagConfigResponse implements IFlagConfigResponse {
  private final Map<String, FlagConfig> flags;
  private final Map<String, BanditReference> banditReferences;
  private final IFlagConfigResponse.Format format;
  private final String environmentName;
  private final Date createdAt;

  public FlagConfigResponse(
      Map<String, FlagConfig> flags,
      Map<String, BanditReference> banditReferences,
      IFlagConfigResponse.Format dataFormat,
      String environmentName,
      Date createdAt) {
    this.flags = flags;
    this.banditReferences = banditReferences;
    this.format = dataFormat;
    this.environmentName = environmentName;
    this.createdAt = createdAt;
  }

  public FlagConfigResponse(
      Map<String, FlagConfig> flags,
      Map<String, BanditReference> banditReferences,
      IFlagConfigResponse.Format dataFormat) {
    this(flags, banditReferences, dataFormat, null, null);
  }

  public FlagConfigResponse(
      Map<String, FlagConfig> flags, Map<String, BanditReference> banditReferences) {
    this(flags, banditReferences, IFlagConfigResponse.Format.SERVER, null, null);
  }

  public FlagConfigResponse() {
    this(
        new ConcurrentHashMap<>(),
        new ConcurrentHashMap<>(),
        IFlagConfigResponse.Format.SERVER,
        null,
        null);
  }

  @Override
  public String toString() {
    return "FlagConfigResponse{"
        + "flags="
        + flags
        + ", banditReferences="
        + banditReferences
        + ", format="
        + format
        + ", environmentName='"
        + environmentName
        + '\''
        + ", createdAt="
        + createdAt
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    FlagConfigResponse that = (FlagConfigResponse) o;
    return Objects.equals(flags, that.flags)
        && Objects.equals(banditReferences, that.banditReferences)
        && format == that.format
        && Objects.equals(environmentName, that.environmentName)
        && Objects.equals(createdAt, that.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(flags, banditReferences, format, environmentName, createdAt);
  }

  public Map<String, FlagConfig> getFlags() {
    return this.flags;
  }

  public Map<String, BanditReference> getBanditReferences() {
    return this.banditReferences;
  }

  @Override
  public IFlagConfigResponse.Format getFormat() {
    return format;
  }

  public String getEnvironmentName() {
    return environmentName;
  }

  public Date getCreatedAt() {
    return createdAt;
  }
}

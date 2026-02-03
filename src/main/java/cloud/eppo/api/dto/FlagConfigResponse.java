package cloud.eppo.api.dto;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public interface FlagConfigResponse {
  Map<String, FlagConfig> getFlags();

  Map<String, BanditReference> getBanditReferences();

  Format getFormat();

  String getEnvironmentName();

  Date getCreatedAt();

  enum Format {
    SERVER,
    CLIENT
  }

  class Default implements FlagConfigResponse {
    private final Map<String, FlagConfig> flags;
    private final Map<String, BanditReference> banditReferences;
    private final Format format;
    private final String environmentName;
    private final Date createdAt;

    public Default(
        Map<String, FlagConfig> flags,
        Map<String, BanditReference> banditReferences,
        Format dataFormat,
        String environmentName,
        Date createdAt) {
      this.flags = flags;
      this.banditReferences = banditReferences;
      this.format = dataFormat;
      this.environmentName = environmentName;
      this.createdAt = createdAt;
    }

    public Default(
        Map<String, FlagConfig> flags,
        Map<String, BanditReference> banditReferences,
        Format dataFormat) {
      this(flags, banditReferences, dataFormat, null, null);
    }

    public Default(Map<String, FlagConfig> flags, Map<String, BanditReference> banditReferences) {
      this(flags, banditReferences, Format.SERVER, null, null);
    }

    public Default() {
      this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), Format.SERVER, null, null);
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
      return Objects.equals(flags, that.getFlags())
          && Objects.equals(banditReferences, that.getBanditReferences())
          && format == that.getFormat()
          && Objects.equals(environmentName, that.getEnvironmentName())
          && Objects.equals(createdAt, that.getCreatedAt());
    }

    @Override
    public int hashCode() {
      return Objects.hash(flags, banditReferences, format, environmentName, createdAt);
    }

    @Override
    public Map<String, FlagConfig> getFlags() {
      return flags;
    }

    @Override
    public Map<String, BanditReference> getBanditReferences() {
      return banditReferences;
    }

    @Override
    public Format getFormat() {
      return format;
    }

    @Override
    public String getEnvironmentName() {
      return environmentName;
    }

    @Override
    public Date getCreatedAt() {
      return createdAt;
    }
  }
}

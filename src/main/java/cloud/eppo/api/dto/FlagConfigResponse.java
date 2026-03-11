package cloud.eppo.api.dto;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Response DTO for parsed flag configuration.
 *
 * <p>This is a transient data structure used during configuration parsing. It is not intended to be
 * persisted or serialized directly. Instead, the {@link cloud.eppo.api.Configuration} class
 * extracts and stores the individual {@link FlagConfig} and {@link BanditReference} objects, which
 * ARE serializable for caching and persistence use cases.
 *
 * <p>Note: This response type intentionally does not implement {@link java.io.Serializable} as it
 * is only used as an intermediate structure during parsing and building of the Configuration
 * object.
 */
public interface FlagConfigResponse {
  @NotNull Map<String, FlagConfig> getFlags();

  @NotNull Map<String, BanditReference> getBanditReferences();

  @NotNull Format getFormat();

  @Nullable String getEnvironmentName();

  @Nullable Date getCreatedAt();

  enum Format {
    SERVER,
    CLIENT
  }

  class Default implements FlagConfigResponse {
    private final @NotNull Map<String, FlagConfig> flags;
    private final @NotNull Map<String, BanditReference> banditReferences;
    private final @NotNull Format format;
    private final @Nullable String environmentName;
    private final @Nullable Date createdAt;

    public Default(
        @Nullable Map<String, FlagConfig> flags,
        @Nullable Map<String, BanditReference> banditReferences,
        @Nullable Format dataFormat,
        @Nullable String environmentName,
        @Nullable Date createdAt) {
      this.flags =
          flags == null
              ? Collections.emptyMap()
              : Collections.unmodifiableMap(new HashMap<>(flags));
      this.banditReferences =
          banditReferences == null
              ? Collections.emptyMap()
              : Collections.unmodifiableMap(new HashMap<>(banditReferences));
      this.format = dataFormat == null ? Format.SERVER : dataFormat;
      this.environmentName = environmentName;
      this.createdAt = createdAt == null ? null : new Date(createdAt.getTime());
    }

    public Default(
        @Nullable Map<String, FlagConfig> flags,
        @Nullable Map<String, BanditReference> banditReferences,
        @Nullable Format dataFormat) {
      this(flags, banditReferences, dataFormat, null, null);
    }

    public Default(
        @Nullable Map<String, FlagConfig> flags,
        @Nullable Map<String, BanditReference> banditReferences) {
      this(flags, banditReferences, Format.SERVER, null, null);
    }

    public Default() {
      this(Collections.emptyMap(), Collections.emptyMap(), Format.SERVER, null, null);
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
    @NotNull public Map<String, FlagConfig> getFlags() {
      return flags;
    }

    @Override
    @NotNull public Map<String, BanditReference> getBanditReferences() {
      return banditReferences;
    }

    @Override
    @NotNull public Format getFormat() {
      return format;
    }

    @Override
    @Nullable public String getEnvironmentName() {
      return environmentName;
    }

    @Override
    @Nullable public Date getCreatedAt() {
      return createdAt == null ? null : new Date(createdAt.getTime());
    }
  }
}

package cloud.eppo.api.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Response DTO for parsed bandit parameters.
 *
 * <p>This is a transient data structure used during configuration parsing. It is not intended to be
 * persisted or serialized directly. Instead, the {@link cloud.eppo.api.Configuration} class
 * extracts and stores the individual {@link BanditParameters} objects, which ARE serializable for
 * caching and persistence use cases.
 *
 * <p>Note: This response type intentionally does not implement {@link java.io.Serializable} as it
 * is only used as an intermediate structure during parsing and building of the Configuration
 * object.
 */
public interface BanditParametersResponse {
  @NotNull Map<String, @NotNull BanditParameters> getBandits();

  class Default implements BanditParametersResponse {
    private final @NotNull Map<String, @NotNull BanditParameters> bandits;

    public Default() {
      this.bandits = Collections.emptyMap();
    }

    public Default(@Nullable Map<String, @NotNull BanditParameters> bandits) {
      this.bandits =
          bandits == null
              ? Collections.emptyMap()
              : Collections.unmodifiableMap(new HashMap<>(bandits));
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
    @NotNull public Map<String, @NotNull BanditParameters> getBandits() {
      return bandits;
    }
  }
}

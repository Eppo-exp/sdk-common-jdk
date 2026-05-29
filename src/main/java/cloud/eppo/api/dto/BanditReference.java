package cloud.eppo.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BanditReference extends Serializable {
  @NotNull String getModelVersion();

  @NotNull List<BanditFlagVariation> getFlagVariations();

  class Default implements BanditReference {
    private static final long serialVersionUID = 1L;
    private final @NotNull String modelVersion;
    private final @NotNull List<BanditFlagVariation> flagVariations;

    public Default(
        @NotNull String modelVersion, @Nullable List<BanditFlagVariation> flagVariations) {
      this.modelVersion = modelVersion;
      this.flagVariations =
          flagVariations == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(flagVariations));
    }

    @Override
    public String toString() {
      return "BanditReference{"
          + "modelVersion='"
          + modelVersion
          + '\''
          + ", flagVariations="
          + flagVariations
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      BanditReference that = (BanditReference) o;
      return Objects.equals(modelVersion, that.getModelVersion())
          && Objects.equals(flagVariations, that.getFlagVariations());
    }

    @Override
    public int hashCode() {
      return Objects.hash(modelVersion, flagVariations);
    }

    @Override
    @NotNull public String getModelVersion() {
      return modelVersion;
    }

    @Override
    @NotNull public List<BanditFlagVariation> getFlagVariations() {
      return flagVariations;
    }
  }
}

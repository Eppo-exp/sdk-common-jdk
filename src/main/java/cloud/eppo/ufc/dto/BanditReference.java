package cloud.eppo.ufc.dto;

import cloud.eppo.api.IBanditReference;
import java.util.List;
import java.util.Objects;

public class BanditReference implements IBanditReference {
  private final String modelVersion;
  private final List<BanditFlagVariation> flagVariations;

  public BanditReference(String modelVersion, List<BanditFlagVariation> flagVariations) {
    this.modelVersion = modelVersion;
    this.flagVariations = flagVariations;
  }

  @Override
  public String toString() {
    return "BanditReference{" +
      "modelVersion='" + modelVersion + '\'' +
      ", flagVariations=" + flagVariations +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BanditReference that = (BanditReference) o;
    return Objects.equals(modelVersion, that.modelVersion)
            && Objects.equals(flagVariations, that.flagVariations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modelVersion, flagVariations);
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public List<BanditFlagVariation> getFlagVariations() {
    return flagVariations;
  }
}

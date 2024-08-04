package cloud.eppo.ufc.dto;

import java.util.List;

public class BanditReference {
  private final String modelVersion;
  private final List<BanditFlagVariation> flagVariations;

  public BanditReference(
      String modelVersion,
      List<BanditFlagVariation> flagVariations) {
    this.modelVersion = modelVersion;
    this.flagVariations = flagVariations;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public List<BanditFlagVariation> getFlagVariations() {
    return flagVariations;
  }
}

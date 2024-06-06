package cloud.eppo.rac.dto;

import java.util.Date;

public class BanditParameters {
  private final String banditKey;
  private final Date updatedAt;
  private final String modelName;
  private final String modelVersion;
  private final BanditModelData modelData;

  public BanditParameters(
      String banditKey,
      Date updatedAt,
      String modelName,
      String modelVersion,
      BanditModelData modelData) {
    this.banditKey = banditKey;
    this.updatedAt = updatedAt;
    this.modelName = modelName;
    this.modelVersion = modelVersion;
    this.modelData = modelData;
  }

  public String getBanditKey() {
    return banditKey;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public String getModelName() {
    return modelName;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public BanditModelData getModelData() {
    return modelData;
  }
}

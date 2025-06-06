package cloud.eppo.ufc.dto;

import java.util.Date;
import java.util.Objects;

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

  @Override
  public String toString() {
    return "BanditParameters{" +
      "banditKey='" + banditKey + '\'' +
      ", updatedAt=" + updatedAt +
      ", modelName='" + modelName + '\'' +
      ", modelVersion='" + modelVersion + '\'' +
      ", modelData=" + modelData +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BanditParameters that = (BanditParameters) o;
    return Objects.equals(banditKey, that.banditKey)
            && Objects.equals(updatedAt, that.updatedAt)
            && Objects.equals(modelName, that.modelName)
            && Objects.equals(modelVersion, that.modelVersion)
            && Objects.equals(modelData, that.modelData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(banditKey, updatedAt, modelName, modelVersion, modelData);
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

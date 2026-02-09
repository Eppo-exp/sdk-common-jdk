package cloud.eppo.api.dto;

import java.util.Date;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BanditParameters {
  @NotNull String getBanditKey();

  @Nullable Date getUpdatedAt();

  @NotNull String getModelName();

  @NotNull String getModelVersion();

  @NotNull BanditModelData getModelData();

  class Default implements BanditParameters {
    private final String banditKey;
    private final Date updatedAt;
    private final String modelName;
    private final String modelVersion;
    private final BanditModelData modelData;

    public Default(
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
      return "BanditParameters{"
          + "banditKey='"
          + banditKey
          + '\''
          + ", updatedAt="
          + updatedAt
          + ", modelName='"
          + modelName
          + '\''
          + ", modelVersion='"
          + modelVersion
          + '\''
          + ", modelData="
          + modelData
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      BanditParameters that = (BanditParameters) o;
      return Objects.equals(banditKey, that.getBanditKey())
          && Objects.equals(updatedAt, that.getUpdatedAt())
          && Objects.equals(modelName, that.getModelName())
          && Objects.equals(modelVersion, that.getModelVersion())
          && Objects.equals(modelData, that.getModelData());
    }

    @Override
    public int hashCode() {
      return Objects.hash(banditKey, updatedAt, modelName, modelVersion, modelData);
    }

    @Override
    public String getBanditKey() {
      return banditKey;
    }

    @Override
    public Date getUpdatedAt() {
      return updatedAt;
    }

    @Override
    public String getModelName() {
      return modelName;
    }

    @Override
    public String getModelVersion() {
      return modelVersion;
    }

    @Override
    public BanditModelData getModelData() {
      return modelData;
    }
  }
}

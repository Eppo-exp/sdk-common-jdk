package cloud.eppo.api.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BanditParameters extends Serializable {
  @NotNull String getBanditKey();

  @Nullable Date getUpdatedAt();

  @NotNull String getModelName();

  @NotNull String getModelVersion();

  @NotNull BanditModelData getModelData();

  class Default implements BanditParameters {
    private static final long serialVersionUID = 1L;
    private final @NotNull String banditKey;
    private final @Nullable Date updatedAt;
    private final @NotNull String modelName;
    private final @NotNull String modelVersion;
    private final @NotNull BanditModelData modelData;

    public Default(
        @NotNull String banditKey,
        @Nullable Date updatedAt,
        @NotNull String modelName,
        @NotNull String modelVersion,
        @NotNull BanditModelData modelData) {
      this.banditKey = banditKey;
      this.updatedAt = updatedAt == null ? null : new Date(updatedAt.getTime());
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
    @NotNull
    public String getBanditKey() {
      return banditKey;
    }

    @Override
    @Nullable
    public Date getUpdatedAt() {
      return updatedAt == null ? null : new Date(updatedAt.getTime());
    }

    @Override
    @NotNull
    public String getModelName() {
      return modelName;
    }

    @Override
    @NotNull
    public String getModelVersion() {
      return modelVersion;
    }

    @Override
    @NotNull
    public BanditModelData getModelData() {
      return modelData;
    }
  }
}

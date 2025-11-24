package cloud.eppo.api;

import java.util.Date;

/**
 * Interface for BanditParameters allowing downstream SDKs to provide custom implementations.
 */
public interface IBanditParameters {
  String getBanditKey();

  Date getUpdatedAt();

  String getModelName();

  String getModelVersion();

  IBanditModelData getModelData();
}

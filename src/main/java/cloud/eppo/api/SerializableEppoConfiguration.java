package cloud.eppo.api;

import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.FlagConfig;
import cloud.eppo.api.dto.VariationType;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public interface SerializableEppoConfiguration extends Serializable {
  FlagConfig getFlag(String flagKey);

  @Nullable VariationType getFlagType(String flagKey);

  String getEnvironmentName();

  Date getConfigFetchedAt();

  Date getConfigPublishedAt();

  boolean isConfigObfuscated();

  String banditKeyForVariation(String flagKey, String variationValue);

  BanditParameters getBanditParameters(String banditKey);

  boolean isEmpty();

  String getFlagsSnapshotId();

  Set<String> getFlagKeys();
}

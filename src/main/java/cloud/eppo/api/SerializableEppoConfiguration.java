package cloud.eppo.api;

import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.FlagConfig;
import cloud.eppo.api.dto.VariationType;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public interface SerializableEppoConfiguration extends Serializable {
  @Nullable FlagConfig getFlag(String flagKey);

  @Nullable VariationType getFlagType(String flagKey);

  @Nullable String getEnvironmentName();

  @Nullable Date getConfigFetchedAt();

  @Nullable Date getConfigPublishedAt();

  boolean isConfigObfuscated();

  @Nullable String banditKeyForVariation(String flagKey, String variationValue);

  @Nullable BanditParameters getBanditParameters(String banditKey);

  boolean isEmpty();

  @Nullable String getFlagsSnapshotId();

  Set<String> getFlagKeys();

  /**
   * Returns true if the configuration references bandit model versions that have not been loaded.
   * Default returns false — safe for configurations that do not use bandits. Override or extend
   * AbstractEppoConfiguration for bandit-aware behavior.
   */
  default boolean requiresUpdatedBanditModels() {
    return false;
  }
}

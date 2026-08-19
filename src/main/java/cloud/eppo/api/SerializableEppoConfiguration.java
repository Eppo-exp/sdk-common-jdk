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
   * Returns the set of bandit model versions referenced by the flag configuration. Used to
   * determine whether fresh bandit parameters need to be fetched.
   */
  Set<String> referencedBanditModelVersions();

  /**
   * Returns the set of bandit model versions currently loaded in this configuration. Used to
   * determine whether fresh bandit parameters need to be fetched.
   */
  Set<String> loadedBanditModelVersions();

  /**
   * Returns true if any referenced bandit model version is not present in the loaded set. Logic
   * lives here once in core so implementations never rewrite it.
   */
  default boolean requiresUpdatedBanditModels() {
    return !loadedBanditModelVersions().containsAll(referencedBanditModelVersions());
  }
}

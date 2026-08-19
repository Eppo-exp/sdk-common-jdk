package cloud.eppo.api;

import java.util.Set;

/**
 * Abstract base class for {@link SerializableEppoConfiguration} implementations that support bandit
 * model version checking.
 *
 * <p>Downstream implementations can either:
 *
 * <ul>
 *   <li>Implement {@link SerializableEppoConfiguration} directly — inherits {@code
 *       requiresUpdatedBanditModels() = false}, appropriate for non-bandit configurations.
 *   <li>Extend this class — implement {@link #referencedBanditModelVersions()} and {@link
 *       #loadedBanditModelVersions()} to provide the data; the comparison logic is provided here.
 * </ul>
 */
public abstract class AbstractEppoConfiguration implements SerializableEppoConfiguration {

  /**
   * Returns the set of bandit model versions referenced by the flag configuration. Implementations
   * derive this from their internal bandit reference data.
   */
  protected abstract Set<String> referencedBanditModelVersions();

  /**
   * Returns the set of bandit model versions currently loaded in this configuration.
   * Implementations derive this from their internal bandit parameter data.
   */
  protected abstract Set<String> loadedBanditModelVersions();

  @Override
  public boolean requiresUpdatedBanditModels() {
    return !loadedBanditModelVersions().containsAll(referencedBanditModelVersions());
  }
}

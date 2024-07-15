package cloud.eppo;

import cloud.eppo.ufc.dto.BanditParameters;
import cloud.eppo.ufc.dto.FlagConfig;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

/** CacheHelper class */
public class CacheHelper {
  private final CacheManager cacheManager;

  public CacheHelper() {
    this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
    this.cacheManager.init();
  }

  /** Create caching for Experiment Configuration */
  public Cache<String, FlagConfig> createExperimentConfigurationCache(int maxEntries) {
    return this.cacheManager.createCache(
        Constants.EXPERIMENT_CONFIGURATION_CACHE_KEY,
        CacheConfigurationBuilder.newCacheConfigurationBuilder(
            String.class, FlagConfig.class, ResourcePoolsBuilder.heap(maxEntries)));
  }

  public Cache<String, BanditParameters> createBanditParameterCache(int maxEntries) {
    return this.cacheManager.createCache(
        Constants.BANDIT_PARAMETER_CACHE_KEY,
        CacheConfigurationBuilder.newCacheConfigurationBuilder(
            String.class, BanditParameters.class, ResourcePoolsBuilder.heap(maxEntries)));
  }
}

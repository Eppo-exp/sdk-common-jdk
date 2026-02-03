package cloud.eppo.http;

import cloud.eppo.ApiEndpoints;
import cloud.eppo.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating configuration requests.
 *
 * <p>This factory encapsulates the logic for constructing URLs and requests for fetching flag
 * configurations and bandit parameters from the Eppo API.
 */
public class EppoConfigurationRequestFactory {

  private final String flagConfigUrl;
  private final String banditParamsUrl;

  /**
   * Creates a new request factory.
   *
   * @param apiEndpoints the API endpoints configuration for determining the base URL
   */
  public EppoConfigurationRequestFactory(@NotNull ApiEndpoints apiEndpoints) {
    String baseUrl = apiEndpoints.getBaseUrl();
    this.flagConfigUrl = baseUrl + Constants.FLAG_CONFIG_ENDPOINT;
    this.banditParamsUrl = baseUrl + Constants.BANDIT_ENDPOINT;
  }

  /**
   * Creates a request for fetching flag configuration.
   *
   * @param lastVersionId optional version identifier for conditional fetch (304 Not Modified
   *     support). If the server's current version matches, a 304 response will be returned.
   * @return the configured request
   */
  public EppoConfigurationRequest createFlagConfigRequest(@Nullable String lastVersionId) {
    EppoConfigurationRequest.Builder builder = EppoConfigurationRequest.builder(flagConfigUrl);
    if (lastVersionId != null) {
      builder.ifNoneMatch(lastVersionId);
    }
    return builder.build();
  }

  /**
   * Creates a request for fetching bandit parameters.
   *
   * @return the configured request
   */
  public EppoConfigurationRequest createBanditParamsRequest() {
    return EppoConfigurationRequest.builder(banditParamsUrl).build();
  }

  /**
   * Returns the URL used for flag configuration requests.
   *
   * @return the flag config URL
   */
  public String getFlagConfigUrl() {
    return flagConfigUrl;
  }

  /**
   * Returns the URL used for bandit parameters requests.
   *
   * @return the bandit params URL
   */
  public String getBanditParamsUrl() {
    return banditParamsUrl;
  }
}

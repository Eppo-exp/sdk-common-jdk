package cloud.eppo.http;

import cloud.eppo.Constants;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating configuration requests.
 *
 * <p>This factory encapsulates the logic for constructing requests for fetching flag configurations
 * and bandit parameters from the Eppo API.
 */
public class EppoConfigurationRequestFactory {

  private final String baseUrl;
  private final Map<String, String> sdkQueryParams;

  /**
   * Creates a new request factory.
   *
   * @param baseUrl the base URL for API requests
   * @param apiKey the API key to include in requests
   * @param sdkName the SDK name to include in requests
   * @param sdkVersion the SDK version to include in requests
   */
  public EppoConfigurationRequestFactory(
      @NotNull String baseUrl,
      @NotNull String apiKey,
      @NotNull String sdkName,
      @NotNull String sdkVersion) {
    this.baseUrl = baseUrl;

    Map<String, String> params = new LinkedHashMap<>();
    params.put("apiKey", apiKey);
    params.put("sdkName", sdkName);
    params.put("sdkVersion", sdkVersion);
    this.sdkQueryParams = Collections.unmodifiableMap(params);
  }

  /**
   * Creates a request for fetching flag configuration.
   *
   * @param lastVersionId optional version identifier for conditional fetch (304 Not Modified
   *     support). If the server's current version matches, a 304 response will be returned.
   * @return the configured request
   */
  @NotNull public EppoConfigurationRequest createFlagConfigRequest(@Nullable String lastVersionId) {
    return new EppoConfigurationRequest(
        baseUrl, Constants.FLAG_CONFIG_ENDPOINT, sdkQueryParams, lastVersionId);
  }

  /**
   * Creates a request for fetching bandit parameters.
   *
   * @return the configured request
   */
  @NotNull public EppoConfigurationRequest createBanditParamsRequest() {
    return new EppoConfigurationRequest(baseUrl, Constants.BANDIT_ENDPOINT, sdkQueryParams, null);
  }
}

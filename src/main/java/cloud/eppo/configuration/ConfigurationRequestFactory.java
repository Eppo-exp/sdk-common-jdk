package cloud.eppo.configuration;

import cloud.eppo.Constants;
import cloud.eppo.api.configuration.ConfigurationRequest;

public class ConfigurationRequestFactory {
  private String url;
  private String apiKey;
  private String sdkName;
  private String sdkVersion;

  public ConfigurationRequestFactory(String url, String apiKey, String sdkName, String sdkVersion) {
    this.url = url;
    this.apiKey = apiKey;
    this.sdkName = sdkName;
    this.sdkVersion = sdkVersion;
  }

  public ConfigurationRequest createConfigurationRequest(String previousETag) {
    return new ConfigurationRequest(url, apiKey, sdkName, sdkVersion, previousETag);
  }

  public ConfigurationRequest createFlagConfigurationRequest(String previousETag) {
    return new ConfigurationRequest(
        url + Constants.FLAG_CONFIG_ENDPOINT, apiKey, sdkName, sdkVersion, previousETag);
  }

  public ConfigurationRequest createBanditConfigurationRequest(String previousETag) {
    return new ConfigurationRequest(
        url + Constants.BANDIT_ENDPOINT, apiKey, sdkName, sdkVersion, previousETag);
  }
}

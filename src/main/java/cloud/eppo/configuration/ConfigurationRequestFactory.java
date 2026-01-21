package cloud.eppo.configuration;

import static cloud.eppo.api.configuration.IEppoConfigurationHttpClient.ConfigurationRequest;

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
}

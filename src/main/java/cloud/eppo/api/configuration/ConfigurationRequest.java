package cloud.eppo.api.configuration;

public class ConfigurationRequest {
  public String url;
  public String apiKey;
  public String sdkName;
  public String sdkVersion;
  public String previousETag;

  public ConfigurationRequest(
      String url, String apiKey, String sdkName, String sdkVersion, String previousETag) {
    this.url = url;
    this.apiKey = apiKey;
    this.sdkName = sdkName;
    this.sdkVersion = sdkVersion;
    this.previousETag = previousETag;
  }
}

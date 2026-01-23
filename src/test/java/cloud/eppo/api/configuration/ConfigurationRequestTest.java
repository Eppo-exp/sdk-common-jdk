package cloud.eppo.api.configuration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ConfigurationRequestTest {

  @Test
  public void testConfigurationRequestConstructor() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key";
    String sdkName = "java-sdk";
    String sdkVersion = "1.0.0";
    String previousETag = "abc123";

    ConfigurationRequest request =
        new ConfigurationRequest(url, apiKey, sdkName, sdkVersion, previousETag);

    assertEquals(url, request.getUrl());
    assertEquals(apiKey, request.getApiKey());
    assertEquals(sdkName, request.getSdkName());
    assertEquals(sdkVersion, request.getSdkVersion());
    assertEquals(previousETag, request.getPreviousETag());
  }

  @Test
  public void testConfigurationRequestWithNullETag() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key";
    String sdkName = "java-sdk";
    String sdkVersion = "1.0.0";

    ConfigurationRequest request = new ConfigurationRequest(url, apiKey, sdkName, sdkVersion, null);

    assertEquals(url, request.getUrl());
    assertEquals(apiKey, request.getApiKey());
    assertEquals(sdkName, request.getSdkName());
    assertEquals(sdkVersion, request.getSdkVersion());
    assertNull(request.getPreviousETag());
  }

  @Test
  public void testConfigurationRequestWithEmptyETag() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key";
    String sdkName = "java-sdk";
    String sdkVersion = "1.0.0";
    String previousETag = "";

    ConfigurationRequest request =
        new ConfigurationRequest(url, apiKey, sdkName, sdkVersion, previousETag);

    assertEquals(url, request.getUrl());
    assertEquals(apiKey, request.getApiKey());
    assertEquals(sdkName, request.getSdkName());
    assertEquals(sdkVersion, request.getSdkVersion());
    assertEquals("", request.getPreviousETag());
  }
}

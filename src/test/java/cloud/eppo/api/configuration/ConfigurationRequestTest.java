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

    assertEquals(url, request.url);
    assertEquals(apiKey, request.apiKey);
    assertEquals(sdkName, request.sdkName);
    assertEquals(sdkVersion, request.sdkVersion);
    assertEquals(previousETag, request.previousETag);
  }

  @Test
  public void testConfigurationRequestWithNullETag() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key";
    String sdkName = "java-sdk";
    String sdkVersion = "1.0.0";

    ConfigurationRequest request = new ConfigurationRequest(url, apiKey, sdkName, sdkVersion, null);

    assertEquals(url, request.url);
    assertEquals(apiKey, request.apiKey);
    assertEquals(sdkName, request.sdkName);
    assertEquals(sdkVersion, request.sdkVersion);
    assertNull(request.previousETag);
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

    assertEquals(url, request.url);
    assertEquals(apiKey, request.apiKey);
    assertEquals(sdkName, request.sdkName);
    assertEquals(sdkVersion, request.sdkVersion);
    assertEquals("", request.previousETag);
  }

  @Test
  public void testConfigurationRequestFieldsAreMutable() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key";
    String sdkName = "java-sdk";
    String sdkVersion = "1.0.0";
    String previousETag = "abc123";

    ConfigurationRequest request =
        new ConfigurationRequest(url, apiKey, sdkName, sdkVersion, previousETag);

    // Verify fields can be mutated
    request.url = "https://new-url.com";
    request.apiKey = "new-api-key";
    request.sdkName = "new-sdk";
    request.sdkVersion = "2.0.0";
    request.previousETag = "xyz789";

    assertEquals("https://new-url.com", request.url);
    assertEquals("new-api-key", request.apiKey);
    assertEquals("new-sdk", request.sdkName);
    assertEquals("2.0.0", request.sdkVersion);
    assertEquals("xyz789", request.previousETag);
  }
}

package cloud.eppo.configuration;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.configuration.ConfigurationRequest;
import org.junit.jupiter.api.Test;

public class ConfigurationRequestFactoryTest {

  @Test
  public void testFactoryCreatesRequestWithAllParameters() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key-123";
    String sdkName = "java-sdk";
    String sdkVersion = "2.5.1";

    ConfigurationRequestFactory factory =
        new ConfigurationRequestFactory(url, apiKey, sdkName, sdkVersion);

    String eTag = "etag-abc123";
    ConfigurationRequest request = factory.createConfigurationRequest(eTag);

    assertEquals(url, request.url);
    assertEquals(apiKey, request.apiKey);
    assertEquals(sdkName, request.sdkName);
    assertEquals(sdkVersion, request.sdkVersion);
    assertEquals(eTag, request.previousETag);
  }

  @Test
  public void testFactoryCreatesRequestWithNullETag() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key-456";
    String sdkName = "kotlin-sdk";
    String sdkVersion = "1.0.0";

    ConfigurationRequestFactory factory =
        new ConfigurationRequestFactory(url, apiKey, sdkName, sdkVersion);

    ConfigurationRequest request = factory.createConfigurationRequest(null);

    assertEquals(url, request.url);
    assertEquals(apiKey, request.apiKey);
    assertEquals(sdkName, request.sdkName);
    assertEquals(sdkVersion, request.sdkVersion);
    assertNull(request.previousETag);
  }

  @Test
  public void testFactoryCreatesRequestWithEmptyETag() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key-789";
    String sdkName = "android-sdk";
    String sdkVersion = "3.2.0";

    ConfigurationRequestFactory factory =
        new ConfigurationRequestFactory(url, apiKey, sdkName, sdkVersion);

    ConfigurationRequest request = factory.createConfigurationRequest("");

    assertEquals(url, request.url);
    assertEquals(apiKey, request.apiKey);
    assertEquals(sdkName, request.sdkName);
    assertEquals(sdkVersion, request.sdkVersion);
    assertEquals("", request.previousETag);
  }

  @Test
  public void testFactoryCreatesMultipleRequestsWithDifferentETags() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key";
    String sdkName = "java-sdk";
    String sdkVersion = "1.0.0";

    ConfigurationRequestFactory factory =
        new ConfigurationRequestFactory(url, apiKey, sdkName, sdkVersion);

    // Create multiple requests with different eTags
    ConfigurationRequest request1 = factory.createConfigurationRequest("etag-1");
    ConfigurationRequest request2 = factory.createConfigurationRequest("etag-2");
    ConfigurationRequest request3 = factory.createConfigurationRequest(null);

    // Verify that all requests have the same base parameters
    assertEquals(url, request1.url);
    assertEquals(url, request2.url);
    assertEquals(url, request3.url);

    assertEquals(apiKey, request1.apiKey);
    assertEquals(apiKey, request2.apiKey);
    assertEquals(apiKey, request3.apiKey);

    // Verify that eTags are different
    assertEquals("etag-1", request1.previousETag);
    assertEquals("etag-2", request2.previousETag);
    assertNull(request3.previousETag);
  }

  @Test
  public void testFactoryWithDifferentUrls() {
    String url1 = "https://api.eppo.cloud/flag-config";
    String url2 = "https://api.eppo.cloud/bandit-config";
    String apiKey = "test-api-key";
    String sdkName = "java-sdk";
    String sdkVersion = "1.0.0";

    ConfigurationRequestFactory factory1 =
        new ConfigurationRequestFactory(url1, apiKey, sdkName, sdkVersion);
    ConfigurationRequestFactory factory2 =
        new ConfigurationRequestFactory(url2, apiKey, sdkName, sdkVersion);

    ConfigurationRequest request1 = factory1.createConfigurationRequest("etag");
    ConfigurationRequest request2 = factory2.createConfigurationRequest("etag");

    assertEquals(url1, request1.url);
    assertEquals(url2, request2.url);
  }

  @Test
  public void testFactoryWithDifferentSdkVersions() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key";
    String sdkName = "java-sdk";

    ConfigurationRequestFactory factory1 =
        new ConfigurationRequestFactory(url, apiKey, sdkName, "1.0.0");
    ConfigurationRequestFactory factory2 =
        new ConfigurationRequestFactory(url, apiKey, sdkName, "2.0.0");
    ConfigurationRequestFactory factory3 =
        new ConfigurationRequestFactory(url, apiKey, sdkName, "3.0.0-SNAPSHOT");

    ConfigurationRequest request1 = factory1.createConfigurationRequest(null);
    ConfigurationRequest request2 = factory2.createConfigurationRequest(null);
    ConfigurationRequest request3 = factory3.createConfigurationRequest(null);

    assertEquals("1.0.0", request1.sdkVersion);
    assertEquals("2.0.0", request2.sdkVersion);
    assertEquals("3.0.0-SNAPSHOT", request3.sdkVersion);
  }

  @Test
  public void testFactoryWithDifferentSdkNames() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "test-api-key";
    String sdkVersion = "1.0.0";

    ConfigurationRequestFactory factory1 =
        new ConfigurationRequestFactory(url, apiKey, "java-sdk", sdkVersion);
    ConfigurationRequestFactory factory2 =
        new ConfigurationRequestFactory(url, apiKey, "kotlin-sdk", sdkVersion);
    ConfigurationRequestFactory factory3 =
        new ConfigurationRequestFactory(url, apiKey, "android-sdk", sdkVersion);

    ConfigurationRequest request1 = factory1.createConfigurationRequest(null);
    ConfigurationRequest request2 = factory2.createConfigurationRequest(null);
    ConfigurationRequest request3 = factory3.createConfigurationRequest(null);

    assertEquals("java-sdk", request1.sdkName);
    assertEquals("kotlin-sdk", request2.sdkName);
    assertEquals("android-sdk", request3.sdkName);
  }

  @Test
  public void testFactoryPreservesOriginalParametersAcrossMultipleCalls() {
    String url = "https://api.eppo.cloud/config";
    String apiKey = "immutable-api-key";
    String sdkName = "immutable-sdk";
    String sdkVersion = "immutable-version";

    ConfigurationRequestFactory factory =
        new ConfigurationRequestFactory(url, apiKey, sdkName, sdkVersion);

    // Create 10 requests and verify they all have the same base parameters
    for (int i = 0; i < 10; i++) {
      ConfigurationRequest request = factory.createConfigurationRequest("etag-" + i);

      assertEquals(url, request.url);
      assertEquals(apiKey, request.apiKey);
      assertEquals(sdkName, request.sdkName);
      assertEquals(sdkVersion, request.sdkVersion);
      assertEquals("etag-" + i, request.previousETag);
    }
  }
}

package cloud.eppo.http;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EppoConfigurationRequestFactoryTest {

  private static final String TEST_API_KEY = "test-api-key";
  private static final String TEST_SDK_NAME = "java-server-sdk";
  private static final String TEST_SDK_VERSION = "1.0.0";
  private EppoConfigurationRequestFactory factory;

  @BeforeEach
  void setUp() {
    factory =
        new EppoConfigurationRequestFactory(
            Constants.DEFAULT_BASE_URL, TEST_API_KEY, TEST_SDK_NAME, TEST_SDK_VERSION);
  }

  @Test
  void testCreateFlagConfigRequestWithoutVersionId() {
    EppoConfigurationRequest request = factory.createFlagConfigRequest(null);

    assertNotNull(request);
    assertEquals(Constants.DEFAULT_BASE_URL, request.getBaseUrl());
    assertEquals(Constants.FLAG_CONFIG_ENDPOINT, request.getResourcePath());
    assertEquals(TEST_API_KEY, request.getQueryParams().get("apiKey"));
    assertEquals(TEST_SDK_NAME, request.getQueryParams().get("sdkName"));
    assertEquals(TEST_SDK_VERSION, request.getQueryParams().get("sdkVersion"));
    assertEquals(3, request.getQueryParams().size());
    assertNull(request.getLastVersionId());
  }

  @Test
  void testCreateFlagConfigRequestWithVersionId() {
    String versionId = "abc123";
    EppoConfigurationRequest request = factory.createFlagConfigRequest(versionId);

    assertNotNull(request);
    assertEquals(Constants.DEFAULT_BASE_URL, request.getBaseUrl());
    assertEquals(Constants.FLAG_CONFIG_ENDPOINT, request.getResourcePath());
    assertEquals(versionId, request.getLastVersionId());
  }

  @Test
  void testCreateBanditParamsRequest() {
    EppoConfigurationRequest request = factory.createBanditParamsRequest();

    assertNotNull(request);
    assertEquals(Constants.DEFAULT_BASE_URL, request.getBaseUrl());
    assertEquals(Constants.BANDIT_ENDPOINT, request.getResourcePath());
    assertEquals(TEST_API_KEY, request.getQueryParams().get("apiKey"));
    assertEquals(TEST_SDK_NAME, request.getQueryParams().get("sdkName"));
    assertEquals(TEST_SDK_VERSION, request.getQueryParams().get("sdkVersion"));
    assertEquals(3, request.getQueryParams().size());
    assertNull(request.getLastVersionId());
  }

  @Test
  void testFactoryWithCustomBaseUrl() {
    String customBaseUrl = "https://custom.example.com";
    EppoConfigurationRequestFactory customFactory =
        new EppoConfigurationRequestFactory(
            customBaseUrl, TEST_API_KEY, TEST_SDK_NAME, TEST_SDK_VERSION);

    EppoConfigurationRequest request = customFactory.createFlagConfigRequest(null);
    assertEquals(customBaseUrl, request.getBaseUrl());
    assertEquals(Constants.FLAG_CONFIG_ENDPOINT, request.getResourcePath());
  }
}

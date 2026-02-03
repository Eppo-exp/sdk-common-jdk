package cloud.eppo.http;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.ApiEndpoints;
import cloud.eppo.Constants;
import cloud.eppo.SDKKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EppoConfigurationRequestFactoryTest {

  private static final String TEST_SDK_KEY = "test-sdk-key";
  private EppoConfigurationRequestFactory factory;

  @BeforeEach
  void setUp() {
    SDKKey sdkKey = new SDKKey(TEST_SDK_KEY);
    ApiEndpoints apiEndpoints = new ApiEndpoints(sdkKey, null);
    factory = new EppoConfigurationRequestFactory(apiEndpoints);
  }

  @Test
  void testGetFlagConfigUrl() {
    String expectedUrl = Constants.DEFAULT_BASE_URL + Constants.FLAG_CONFIG_ENDPOINT;
    assertEquals(expectedUrl, factory.getFlagConfigUrl());
  }

  @Test
  void testGetBanditParamsUrl() {
    String expectedUrl = Constants.DEFAULT_BASE_URL + Constants.BANDIT_ENDPOINT;
    assertEquals(expectedUrl, factory.getBanditParamsUrl());
  }

  @Test
  void testCreateFlagConfigRequestWithoutVersionId() {
    EppoConfigurationRequest request = factory.createFlagConfigRequest(null);

    assertNotNull(request);
    assertEquals(factory.getFlagConfigUrl(), request.getUri());
    assertNull(request.getLastVersionId());
    assertTrue(request.getQueryParams().isEmpty());
  }

  @Test
  void testCreateFlagConfigRequestWithVersionId() {
    String versionId = "abc123";
    EppoConfigurationRequest request = factory.createFlagConfigRequest(versionId);

    assertNotNull(request);
    assertEquals(factory.getFlagConfigUrl(), request.getUri());
    assertEquals(versionId, request.getLastVersionId());
  }

  @Test
  void testCreateBanditParamsRequest() {
    EppoConfigurationRequest request = factory.createBanditParamsRequest();

    assertNotNull(request);
    assertEquals(factory.getBanditParamsUrl(), request.getUri());
    assertNull(request.getLastVersionId());
    assertTrue(request.getQueryParams().isEmpty());
  }

  @Test
  void testFactoryWithCustomBaseUrl() {
    String customBaseUrl = "https://custom.example.com";
    SDKKey sdkKey = new SDKKey(TEST_SDK_KEY);
    ApiEndpoints apiEndpoints = new ApiEndpoints(sdkKey, customBaseUrl);
    EppoConfigurationRequestFactory customFactory =
        new EppoConfigurationRequestFactory(apiEndpoints);

    assertEquals(customBaseUrl + Constants.FLAG_CONFIG_ENDPOINT, customFactory.getFlagConfigUrl());
    assertEquals(customBaseUrl + Constants.BANDIT_ENDPOINT, customFactory.getBanditParamsUrl());
  }
}

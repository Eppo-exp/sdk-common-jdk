package cloud.eppo.helpers;

import cloud.eppo.ApiEndpoints;
import cloud.eppo.Constants;
import cloud.eppo.SDKKey;
import cloud.eppo.http.EppoConfigurationRequestFactory;
import cloud.eppo.parser.ConfigurationParser;

/** Test utilities for Eppo SDK tests. */
public class TestUtils {

  /** Test SDK key for use in tests. */
  public static final String TEST_SDK_KEY = "test-sdk-key";

  /** Test SDK key instance. */
  public static final SDKKey TEST_SDK_KEY_INSTANCE = new SDKKey(TEST_SDK_KEY);

  /**
   * Creates a test HTTP client with pre-configured responses.
   *
   * @param flagConfigResponse the response body for flag config requests
   * @return a configured TestHttpClient
   */
  public static TestHttpClient createTestHttpClient(String flagConfigResponse) {
    return createTestHttpClient(flagConfigResponse, null);
  }

  /**
   * Creates a test HTTP client with pre-configured responses.
   *
   * @param flagConfigResponse the response body for flag config requests
   * @param banditParamsResponse the response body for bandit params requests (nullable)
   * @return a configured TestHttpClient
   */
  public static TestHttpClient createTestHttpClient(
      String flagConfigResponse, String banditParamsResponse) {
    TestHttpClient client = new TestHttpClient();
    client.setResponse(Constants.FLAG_CONFIG_ENDPOINT, flagConfigResponse);
    if (banditParamsResponse != null) {
      client.setResponse(Constants.BANDIT_ENDPOINT, banditParamsResponse);
    }
    return client;
  }

  /**
   * Creates a test HTTP client that throws errors.
   *
   * @return a configured TestHttpClient that throws errors
   */
  public static TestHttpClient createErrorHttpClient() {
    TestHttpClient client = new TestHttpClient();
    client.setError(new RuntimeException("Intentional Error"));
    return client;
  }

  /**
   * Creates a test configuration parser using Jackson.
   *
   * @return a TestConfigurationParser instance
   */
  public static ConfigurationParser createTestParser() {
    return new TestConfigurationParser();
  }

  /**
   * Creates a test configuration request factory.
   *
   * @return a configured EppoConfigurationRequestFactory
   */
  public static EppoConfigurationRequestFactory createTestRequestFactory() {
    ApiEndpoints apiEndpoints = new ApiEndpoints(TEST_SDK_KEY_INSTANCE, null);
    return new EppoConfigurationRequestFactory(apiEndpoints);
  }
}

package cloud.eppo.configuration;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.configuration.ConfigurationRequest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ConfigurationRequestFactoryTest {

  private static final String DEFAULT_URL = "https://api.eppo.cloud/config";
  private static final String DEFAULT_API_KEY = "test-api-key";
  private static final String DEFAULT_SDK_NAME = "java-sdk";
  private static final String DEFAULT_SDK_VERSION = "1.0.0";

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideFactoryTestCases")
  public void testFactoryCreatesRequestWithVariousParameters(
      String testName, String url, String apiKey, String sdkName, String sdkVersion, String eTag) {
    ConfigurationRequestFactory factory =
        new ConfigurationRequestFactory(url, apiKey, sdkName, sdkVersion);

    ConfigurationRequest request = factory.createConfigurationRequest(eTag);

    assertRequestMatches(request, url, apiKey, sdkName, sdkVersion, eTag);
  }

  private static Stream<Arguments> provideFactoryTestCases() {
    return Stream.of(
        // ETag variations with default parameters
        Arguments.of(
            "with non-null eTag",
            DEFAULT_URL,
            DEFAULT_API_KEY,
            DEFAULT_SDK_NAME,
            DEFAULT_SDK_VERSION,
            "etag-abc123"),
        Arguments.of(
            "with null eTag",
            DEFAULT_URL,
            DEFAULT_API_KEY,
            DEFAULT_SDK_NAME,
            DEFAULT_SDK_VERSION,
            null),
        Arguments.of(
            "with empty eTag",
            DEFAULT_URL,
            DEFAULT_API_KEY,
            DEFAULT_SDK_NAME,
            DEFAULT_SDK_VERSION,
            ""),
        // URL variations
        Arguments.of(
            "different URL - flag-config",
            "https://api.eppo.cloud/flag-config",
            DEFAULT_API_KEY,
            DEFAULT_SDK_NAME,
            DEFAULT_SDK_VERSION,
            null),
        Arguments.of(
            "different URL - bandit-config",
            "https://api.eppo.cloud/bandit-config",
            DEFAULT_API_KEY,
            DEFAULT_SDK_NAME,
            DEFAULT_SDK_VERSION,
            null),
        // SDK name variations
        Arguments.of(
            "different SDK name - kotlin",
            DEFAULT_URL,
            DEFAULT_API_KEY,
            "kotlin-sdk",
            DEFAULT_SDK_VERSION,
            null),
        Arguments.of(
            "different SDK name - android",
            DEFAULT_URL,
            DEFAULT_API_KEY,
            "android-sdk",
            DEFAULT_SDK_VERSION,
            null),
        // SDK version variations
        Arguments.of(
            "different version 1.0.0",
            DEFAULT_URL,
            DEFAULT_API_KEY,
            DEFAULT_SDK_NAME,
            "1.0.0",
            null),
        Arguments.of(
            "different version 2.0.0",
            DEFAULT_URL,
            DEFAULT_API_KEY,
            DEFAULT_SDK_NAME,
            "2.0.0",
            null),
        Arguments.of(
            "different version SNAPSHOT",
            DEFAULT_URL,
            DEFAULT_API_KEY,
            DEFAULT_SDK_NAME,
            "3.0.0-SNAPSHOT",
            null));
  }

  private void assertRequestMatches(
      ConfigurationRequest request,
      String expectedUrl,
      String expectedApiKey,
      String expectedSdkName,
      String expectedSdkVersion,
      String expectedETag) {
    assertEquals(expectedUrl, request.getUrl());
    assertEquals(expectedApiKey, request.getApiKey());
    assertEquals(expectedSdkName, request.getSdkName());
    assertEquals(expectedSdkVersion, request.getSdkVersion());
    assertEquals(expectedETag, request.getPreviousETag());
  }
}

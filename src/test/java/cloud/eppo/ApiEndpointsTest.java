package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.helpers.JavaBase64Codec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ApiEndpointsTest {
  final SDKKey plainKey = new SDKKey("flat token");

  @BeforeAll
  public static void setUp() {
    Utils.setBase64Codec(new JavaBase64Codec());
  }

  @Test
  public void testDefaultBaseUrl() {
    ApiEndpoints endpoints = new ApiEndpoints(plainKey, null);
    assertEquals("https://fscdn.eppo.cloud/api", endpoints.getBaseUrl());
  }

  @Test
  public void testCustomBaseUrl() {
    ApiEndpoints endpoints = new ApiEndpoints(plainKey, "custom.domain/api");
    assertEquals("https://custom.domain/api", endpoints.getBaseUrl());
  }

  @Test
  public void testCustomBaseUrlWithProtocol() {
    ApiEndpoints endpoints = new ApiEndpoints(plainKey, "http://custom.domain/api");
    assertEquals("http://custom.domain/api", endpoints.getBaseUrl());
  }

  @Test
  public void testCustomBaseUrlWithPort() {
    ApiEndpoints endpoints = new ApiEndpoints(plainKey, "http://custom.domain/api:1337");
    assertEquals("http://custom.domain/api:1337", endpoints.getBaseUrl());
  }

  @Test
  public void testCustomBaseUrlWithProtocolRelative() {
    ApiEndpoints endpoints = new ApiEndpoints(plainKey, "//custom.domain/api");
    assertEquals("//custom.domain/api", endpoints.getBaseUrl());
  }

  @Test
  public void testCustomBaseUrlWithTrailingSlash() {
    ApiEndpoints endpoints = new ApiEndpoints(plainKey, "custom.domain/api/");
    assertEquals("https://custom.domain/api", endpoints.getBaseUrl());
  }

  @Test
  public void testSubdomainFromToken() {
    String payload = "cs=test-subdomain";
    String encodedPayload = Utils.base64Encode(payload);
    String token = "signature." + encodedPayload;

    SDKKey sdkKey = new SDKKey(token);
    ApiEndpoints endpoints = new ApiEndpoints(sdkKey, null);

    assertEquals("https://test-subdomain.fscdn.eppo.cloud/api", endpoints.getBaseUrl());
  }

  @Test
  public void testCustomBaseUrlTakesPrecedenceOverSubdomain() {
    String payload = "cs=test-subdomain";
    String encodedPayload = Utils.base64Encode(payload);
    String token = "signature." + encodedPayload;

    SDKKey decoder = new SDKKey(token);
    ApiEndpoints endpoints = new ApiEndpoints(decoder, "custom.domain/api");

    assertEquals("https://custom.domain/api", endpoints.getBaseUrl());
  }

  @Test
  public void testMultipleTrailingSlashes() {
    ApiEndpoints endpoints = new ApiEndpoints(plainKey, "custom.domain/api////");
    assertEquals("https://custom.domain/api", endpoints.getBaseUrl());
  }

  @Test
  public void testInvalidToken() {
    SDKKey sdkKey = new SDKKey("invalid-token");
    ApiEndpoints endpoints = new ApiEndpoints(sdkKey, null);

    assertEquals("https://fscdn.eppo.cloud/api", endpoints.getBaseUrl());
  }
}

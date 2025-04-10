package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SDKKeyTest {

  @Test
  public void testValidToken() {
    // Create a valid token with encoded subdomain
    String payload = "cs=test-subdomain";
    String encodedPayload = Utils.base64Encode(payload);
    String token = "signature." + encodedPayload;

    SDKKey sdkKey = new SDKKey(token);

    assertTrue(sdkKey.isValid());
    assertEquals("test-subdomain", sdkKey.getSubdomain());
    assertEquals(token, sdkKey.getToken());
  }

  @Test
  public void testInvalidToken() {
    SDKKey sdkKey = new SDKKey("invalid-token");

    assertFalse(sdkKey.isValid());
    assertNull(sdkKey.getSubdomain());
    assertEquals("invalid-token", sdkKey.getToken());
  }

  @Test
  public void testEmptyToken() {
    SDKKey sdkKey = new SDKKey("");

    assertFalse(sdkKey.isValid());
    assertNull(sdkKey.getSubdomain());
    assertEquals("", sdkKey.getToken());
  }

  @Test
  public void testTokenWithoutSubdomain() {
    String payload = "other=value";
    String encodedPayload = Utils.base64Encode(payload);
    String token = "signature." + encodedPayload;

    SDKKey sdkKey = new SDKKey(token);

    // Key is valid with any encoded data.
    assertTrue(sdkKey.isValid());
    assertNull(sdkKey.getSubdomain());
    assertEquals(token, sdkKey.getToken());
  }

  @Test
  public void testTokenWithMultipleParams() {
    String payload = "cs=test-subdomain&other=value";
    String encodedPayload = Utils.base64Encode(payload);
    String token = "signature." + encodedPayload;

    SDKKey sdkKey = new SDKKey(token);

    assertTrue(sdkKey.isValid());
    assertEquals("test-subdomain", sdkKey.getSubdomain());
    assertEquals(token, sdkKey.getToken());
  }

  @Test
  public void testTokenWithEncodedCharacters() {
    String payload = "cs=test%20subdomain&other=special%26value";
    String encodedPayload = Utils.base64Encode(payload);
    String token = "signature." + encodedPayload;

    SDKKey sdkKey = new SDKKey(token);

    assertTrue(sdkKey.isValid());
    assertEquals("test subdomain", sdkKey.getSubdomain());
    assertEquals(token, sdkKey.getToken());
  }

  @Test
  public void testTokenWithMalformedBase64() {
    String token = "signature.not-valid-base64";

    SDKKey sdkKey = new SDKKey(token);

    assertFalse(sdkKey.isValid());
    assertNull(sdkKey.getSubdomain());
    assertEquals(token, sdkKey.getToken());
  }
}

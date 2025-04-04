package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SdkKeyTest {

  @Test
  public void testValidToken() {
    // Create a valid token with encoded subdomain
    String payload = "cs=test-subdomain";
    String encodedPayload = Utils.base64Encode(payload);
    String token = "signature." + encodedPayload;

    SdkKey decoder = new SdkKey(token);

    assertTrue(decoder.isValid());
    assertEquals("test-subdomain", decoder.getSubdomain());
    assertEquals(token, decoder.getToken());
  }

  @Test
  public void testInvalidToken() {
    SdkKey decoder = new SdkKey("invalid-token");

    assertFalse(decoder.isValid());
    assertNull(decoder.getSubdomain());
    assertEquals("invalid-token", decoder.getToken());
  }

  @Test
  public void testEmptyToken() {
    SdkKey decoder = new SdkKey("");

    assertFalse(decoder.isValid());
    assertNull(decoder.getSubdomain());
    assertEquals("", decoder.getToken());
  }

  @Test
  public void testTokenWithoutSubdomain() {
    String payload = "other=value";
    String encodedPayload = Utils.base64Encode(payload);
    String token = "signature." + encodedPayload;

    SdkKey decoder = new SdkKey(token);

    assertFalse(decoder.isValid());
    assertNull(decoder.getSubdomain());
    assertEquals(token, decoder.getToken());
  }

  @Test
  public void testTokenWithMultipleParams() {
    String payload = "cs=test-subdomain&other=value";
    String encodedPayload = Utils.base64Encode(payload);
    String token = "signature." + encodedPayload;

    SdkKey decoder = new SdkKey(token);

    assertTrue(decoder.isValid());
    assertEquals("test-subdomain", decoder.getSubdomain());
    assertEquals(token, decoder.getToken());
  }

  @Test
  public void testTokenWithEncodedCharacters() {
    String payload = "cs=test%20subdomain&other=special%26value";
    String encodedPayload = Utils.base64Encode(payload);
    String token = "signature." + encodedPayload;

    SdkKey decoder = new SdkKey(token);

    assertTrue(decoder.isValid());
    assertEquals("test subdomain", decoder.getSubdomain());
    assertEquals(token, decoder.getToken());
  }

  @Test
  public void testTokenWithMalformedBase64() {
    String token = "signature.not-valid-base64";

    SdkKey decoder = new SdkKey(token);

    assertFalse(decoder.isValid());
    assertNull(decoder.getSubdomain());
    assertEquals(token, decoder.getToken());
  }
}

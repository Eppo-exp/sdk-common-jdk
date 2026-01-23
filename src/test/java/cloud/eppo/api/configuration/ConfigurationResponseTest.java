package cloud.eppo.api.configuration;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.IBanditParametersResponse;
import cloud.eppo.api.IFlagConfigResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ConfigurationResponseTest {

  @Test
  public void testFlagsSuccessResponse() {
    IFlagConfigResponse mockPayload = Mockito.mock(IFlagConfigResponse.class);
    String eTag = "test-etag-123";

    ConfigurationResponse<IFlagConfigResponse> response =
        ConfigurationResponse.Flags.success(mockPayload, eTag);

    assertEquals(mockPayload, response.payload);
    assertEquals(eTag, response.eTag);
    assertEquals(200, response.statusCode);
    assertNull(response.errorMessage);
    assertTrue(response.isSuccess());
    assertFalse(response.isNotModified());
    assertFalse(response.isError());
  }

  @Test
  public void testFlagsSuccessResponseWithNullETag() {
    IFlagConfigResponse mockPayload = Mockito.mock(IFlagConfigResponse.class);

    ConfigurationResponse<IFlagConfigResponse> response =
        ConfigurationResponse.Flags.success(mockPayload, null);

    assertEquals(mockPayload, response.payload);
    assertNull(response.eTag);
    assertEquals(200, response.statusCode);
    assertNull(response.errorMessage);
    assertTrue(response.isSuccess());
  }

  @Test
  public void testFlagsNotModifiedResponse() {
    String eTag = "existing-etag-456";

    ConfigurationResponse<IFlagConfigResponse> response =
        ConfigurationResponse.Flags.notModified(eTag);

    assertNull(response.payload);
    assertEquals(eTag, response.eTag);
    assertEquals(304, response.statusCode);
    assertNull(response.errorMessage);
    assertTrue(response.isNotModified());
    assertFalse(response.isSuccess());
    assertFalse(response.isError());
  }

  @Test
  public void testFlagsErrorResponse() {
    int statusCode = 500;
    String errorMessage = "Internal Server Error";

    ConfigurationResponse<IFlagConfigResponse> response =
        ConfigurationResponse.Flags.error(statusCode, errorMessage);

    assertNull(response.payload);
    assertNull(response.eTag);
    assertEquals(500, response.statusCode);
    assertEquals(errorMessage, response.errorMessage);
    assertTrue(response.isError());
    assertFalse(response.isSuccess());
    assertFalse(response.isNotModified());
  }

  @Test
  public void testFlagsErrorResponseWithDifferentStatusCodes() {
    // Test various error status codes
    int[] errorCodes = {400, 401, 403, 404, 500, 502, 503};

    for (int code : errorCodes) {
      ConfigurationResponse<IFlagConfigResponse> response =
          ConfigurationResponse.Flags.error(code, "Error " + code);

      assertEquals(code, response.statusCode);
      assertTrue(response.isError(), "Status code " + code + " should be an error");
      assertFalse(response.isSuccess());
      assertFalse(response.isNotModified());
    }
  }

  @Test
  public void testBanditsSuccessResponse() {
    IBanditParametersResponse mockPayload = Mockito.mock(IBanditParametersResponse.class);
    String eTag = "bandit-etag-789";

    ConfigurationResponse<IBanditParametersResponse> response =
        ConfigurationResponse.Bandits.success(mockPayload, eTag);

    assertEquals(mockPayload, response.payload);
    assertEquals(eTag, response.eTag);
    assertEquals(200, response.statusCode);
    assertNull(response.errorMessage);
    assertTrue(response.isSuccess());
    assertFalse(response.isNotModified());
    assertFalse(response.isError());
  }

  @Test
  public void testBanditsNotModifiedResponse() {
    String eTag = "bandit-existing-etag";

    ConfigurationResponse<IBanditParametersResponse> response =
        ConfigurationResponse.Bandits.notModified(eTag);

    assertNull(response.payload);
    assertEquals(eTag, response.eTag);
    assertEquals(304, response.statusCode);
    assertNull(response.errorMessage);
    assertTrue(response.isNotModified());
    assertFalse(response.isSuccess());
    assertFalse(response.isError());
  }

  @Test
  public void testBanditsErrorResponse() {
    int statusCode = 404;
    String errorMessage = "Not Found";

    ConfigurationResponse<IBanditParametersResponse> response =
        ConfigurationResponse.Bandits.error(statusCode, errorMessage);

    assertNull(response.payload);
    assertNull(response.eTag);
    assertEquals(404, response.statusCode);
    assertEquals(errorMessage, response.errorMessage);
    assertTrue(response.isError());
    assertFalse(response.isSuccess());
    assertFalse(response.isNotModified());
  }

  @Test
  public void testIsNotModifiedOnlyTrueFor304() {
    // Test that only 304 returns true for isNotModified()
    assertTrue(ConfigurationResponse.Flags.notModified("etag").isNotModified());
    assertFalse(ConfigurationResponse.Flags.success(null, "etag").isNotModified());
    assertFalse(ConfigurationResponse.Flags.error(200, "error").isNotModified());
    assertFalse(ConfigurationResponse.Flags.error(400, "error").isNotModified());
    assertFalse(ConfigurationResponse.Flags.error(500, "error").isNotModified());
  }

  @Test
  public void testIsSuccessOnlyTrueFor200() {
    // Test that only 200 returns true for isSuccess()
    assertTrue(ConfigurationResponse.Flags.success(null, "etag").isSuccess());
    assertFalse(ConfigurationResponse.Flags.notModified("etag").isSuccess());
    assertFalse(ConfigurationResponse.Flags.error(201, "error").isSuccess());
    assertFalse(ConfigurationResponse.Flags.error(400, "error").isSuccess());
    assertFalse(ConfigurationResponse.Flags.error(500, "error").isSuccess());
  }

  @Test
  public void testIsErrorFalseFor200And304() {
    // Test that 200 and 304 return false for isError()
    assertFalse(ConfigurationResponse.Flags.success(null, "etag").isError());
    assertFalse(ConfigurationResponse.Flags.notModified("etag").isError());

    // Test that other codes return true for isError()
    assertTrue(ConfigurationResponse.Flags.error(201, "error").isError());
    assertTrue(ConfigurationResponse.Flags.error(400, "error").isError());
    assertTrue(ConfigurationResponse.Flags.error(401, "error").isError());
    assertTrue(ConfigurationResponse.Flags.error(403, "error").isError());
    assertTrue(ConfigurationResponse.Flags.error(404, "error").isError());
    assertTrue(ConfigurationResponse.Flags.error(500, "error").isError());
  }

  @Test
  public void testResponseFieldsAreMutable() {
    IFlagConfigResponse mockPayload = Mockito.mock(IFlagConfigResponse.class);
    ConfigurationResponse<IFlagConfigResponse> response =
        ConfigurationResponse.Flags.success(mockPayload, "etag");

    // Verify fields can be mutated
    IFlagConfigResponse newPayload = Mockito.mock(IFlagConfigResponse.class);
    response.payload = newPayload;
    response.eTag = "new-etag";
    response.statusCode = 304;
    response.errorMessage = "Some error";

    assertEquals(newPayload, response.payload);
    assertEquals("new-etag", response.eTag);
    assertEquals(304, response.statusCode);
    assertEquals("Some error", response.errorMessage);
  }

  @Test
  public void testNullErrorMessage() {
    ConfigurationResponse<IFlagConfigResponse> response =
        ConfigurationResponse.Flags.error(500, null);

    assertNull(response.errorMessage);
    assertTrue(response.isError());
  }

  @Test
  public void testEmptyErrorMessage() {
    ConfigurationResponse<IFlagConfigResponse> response =
        ConfigurationResponse.Flags.error(500, "");

    assertEquals("", response.errorMessage);
    assertTrue(response.isError());
  }
}

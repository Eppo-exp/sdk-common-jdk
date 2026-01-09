package cloud.eppo.api;

import static cloud.eppo.Utils.getMD5Hex;
import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.ufc.dto.FlagConfig;
import cloud.eppo.ufc.dto.FlagConfigResponse;
import cloud.eppo.ufc.dto.VariationType;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

public class ConfigurationBuilderTest {

  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(EppoModule.eppoModule());

  @Test
  public void testHydrateConfigFromBytesForServer_true() {
    byte[] jsonBytes = "{ \"format\": \"SERVER\", \"flags\":{} }".getBytes();
    Configuration config = new Configuration.Builder(jsonBytes).build();
    assertFalse(config.isConfigObfuscated());
  }

  @Test
  public void testHydrateConfigFromBytesForServer_false() {
    byte[] jsonBytes = "{ \"format\": \"CLIENT\", \"flags\":{} }".getBytes();
    Configuration config = new Configuration.Builder(jsonBytes).build();
    assertTrue(config.isConfigObfuscated());
  }

  @Test
  public void testBuildConfigAutoDetectsServerFormat() throws IOException {
    byte[] jsonBytes = "{ \"flags\":{}, \"format\": \"SERVER\" }".getBytes();
    Configuration config = Configuration.builder(jsonBytes).build();
    assertFalse(config.isConfigObfuscated());

    byte[] serializedFlags = config.serializeFlagConfigToBytes();
    FlagConfigResponse rehydratedConfig =
        mapper.readValue(serializedFlags, FlagConfigResponse.class);

    assertEquals(rehydratedConfig.getFormat(), FlagConfigResponse.Format.SERVER);
  }

  @Test
  public void testBuildConfigAutoDetectsClientFormat() throws IOException {
    byte[] jsonBytes = "{ \"flags\":{}, \"format\": \"CLIENT\" }".getBytes();
    Configuration config = Configuration.builder(jsonBytes).build();
    assertTrue(config.isConfigObfuscated());

    byte[] serializedFlags = config.serializeFlagConfigToBytes();
    FlagConfigResponse rehydratedConfig =
        mapper.readValue(serializedFlags, FlagConfigResponse.class);

    assertEquals(rehydratedConfig.getFormat(), FlagConfigResponse.Format.CLIENT);
  }

  @Test
  public void getFlagType_shouldReturnCorrectType() {
    // Create a flag config with a STRING variation type
    FlagConfig flagConfig =
        new FlagConfig(
            "test-flag",
            true,
            1,
            VariationType.STRING,
            Collections.emptyMap(),
            Collections.emptyList());

    // Create configuration with this flag
    Map<String, FlagConfig> flags = Collections.singletonMap("test-flag", flagConfig);
    Configuration config =
        new Configuration(
            flags,
            Collections.emptyMap(),
            Collections.emptyMap(),
            false,
            null, // environmentName
            null, // configFetchedAt
            null, // configPublishedAt
            null, // flagConfigJson
            null); // banditParamsJson

    // Test successful case
    assertEquals(VariationType.STRING, config.getFlagType("test-flag"));

    // Test non-existent flag
    assertNull(config.getFlagType("non-existent-flag"));
  }

  @Test
  public void getFlagType_withObfuscatedConfig_shouldReturnCorrectType() {
    // Create a flag config with a NUMERIC variation type
    FlagConfig flagConfig =
        new FlagConfig(
            "test-flag",
            true,
            1,
            VariationType.NUMERIC,
            Collections.emptyMap(),
            Collections.emptyList());

    // Create configuration with this flag using MD5 hash of the flag key
    String hashedKey = getMD5Hex("test-flag");
    Map<String, FlagConfig> flags = Collections.singletonMap(hashedKey, flagConfig);
    Configuration config =
        new Configuration(
            flags,
            Collections.emptyMap(),
            Collections.emptyMap(),
            true, // obfuscated
            null, // environmentName
            null, // configFetchedAt
            null, // configPublishedAt
            null, // flagConfigJson
            null); // banditParamsJson

    // Test successful case with obfuscated config
    assertEquals(VariationType.NUMERIC, config.getFlagType("test-flag"));

    // Test non-existent flag
    assertNull(config.getFlagType("non-existent-flag"));
    assertNull(config.getFlagType(hashedKey));
  }

  @Test
  public void getFlagType_withEmptyConfig_shouldReturnNull() {
    Configuration config = Configuration.emptyConfig();
    assertNull(config.getFlagType("any-flag"));
  }

  @Test
  public void testEnvironmentNameParsedFromJson() {
    // Environment name is nested inside an "environment" object
    String json =
        "{ \"flags\": {}, \"environment\": { \"name\": \"Production\" }, \"createdAt\": \"2024-01-01T00:00:00.000Z\" }";
    Configuration config = new Configuration.Builder(json.getBytes()).build();

    assertEquals("Production", config.getEnvironmentName());
  }

  @Test
  public void testEnvironmentNameNullWhenNotInJson() {
    // When flags are present but no environment object
    String json = "{ \"flags\": {} }";
    Configuration config = new Configuration.Builder(json.getBytes()).build();

    assertNull(config.getEnvironmentName());
  }

  @Test
  public void testConfigPublishedAtParsedFromCreatedAt() throws Exception {
    String json = "{ \"flags\": {}, \"createdAt\": \"2024-04-17T19:40:53.716Z\" }";
    Configuration config = new Configuration.Builder(json.getBytes()).build();

    // configPublishedAt should be set from the createdAt field in the JSON
    Date publishedAt = config.getConfigPublishedAt();
    assertNotNull(publishedAt, "configPublishedAt should be set from createdAt");

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date expectedDate = sdf.parse("2024-04-17T19:40:53.716Z");
    assertEquals(expectedDate, publishedAt);
  }

  @Test
  public void testConfigPublishedAtNullWhenCreatedAtNotInJson() {
    String json = "{ \"flags\": {} }";
    Configuration config = new Configuration.Builder(json.getBytes()).build();

    assertNull(config.getConfigPublishedAt());
  }

  @Test
  public void testConfigFetchedAtSetOnBuild() throws InterruptedException {
    String json = "{ \"flags\": {} }";

    Date beforeBuild = new Date();
    // Small sleep to ensure time difference is measurable
    Thread.sleep(10);

    Configuration config = new Configuration.Builder(json.getBytes()).build();

    Thread.sleep(10);
    Date afterBuild = new Date();

    Date fetchedAt = config.getConfigFetchedAt();
    assertNotNull(fetchedAt, "configFetchedAt should be set when build() is called");

    // fetchedAt should be between beforeBuild and afterBuild
    assertFalse(
        fetchedAt.before(beforeBuild),
        "configFetchedAt should not be before the build was started");
    assertFalse(
        fetchedAt.after(afterBuild), "configFetchedAt should not be after the build completed");
  }

  @Test
  public void testAllMetadataFieldsTogether() throws Exception {
    String json =
        "{ \"flags\": {}, \"environment\": { \"name\": \"Staging\" }, \"createdAt\": \"2024-06-15T12:30:00.000Z\", \"format\": \"SERVER\" }";

    Date beforeBuild = new Date();
    Configuration config = new Configuration.Builder(json.getBytes()).build();
    Date afterBuild = new Date();

    // Verify environmentName
    assertEquals("Staging", config.getEnvironmentName());

    // Verify configPublishedAt (from createdAt)
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date expectedPublishedAt = sdf.parse("2024-06-15T12:30:00.000Z");
    assertEquals(expectedPublishedAt, config.getConfigPublishedAt());

    // Verify configFetchedAt is set at build time
    assertNotNull(config.getConfigFetchedAt());
    assertFalse(config.getConfigFetchedAt().before(beforeBuild));
    assertFalse(config.getConfigFetchedAt().after(afterBuild));

    // Verify obfuscation setting
    assertFalse(config.isConfigObfuscated());
  }

  @Test
  public void testEmptyConfigHasNullMetadata() {
    Configuration config = Configuration.emptyConfig();

    assertNull(config.getEnvironmentName());
    assertNull(config.getConfigFetchedAt());
    assertNull(config.getConfigPublishedAt());
  }
}

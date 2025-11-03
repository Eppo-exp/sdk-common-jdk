package cloud.eppo.api;

import static cloud.eppo.Utils.getMD5Hex;
import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.ufc.dto.FlagConfig;
import cloud.eppo.ufc.dto.FlagConfigResponse;
import cloud.eppo.ufc.dto.VariationType;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
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
  public void testBuildConfigAddsForServer_true() throws IOException {
    byte[] jsonBytes = "{ \"flags\":{} }".getBytes();
    Configuration config = Configuration.builder(jsonBytes, false).build();
    assertFalse(config.isConfigObfuscated());

    byte[] serializedFlags = config.serializeFlagConfigToBytes();
    FlagConfigResponse rehydratedConfig =
        mapper.readValue(serializedFlags, FlagConfigResponse.class);

    assertEquals(rehydratedConfig.getFormat(), FlagConfigResponse.Format.SERVER);
  }

  @Test
  public void testBuildConfigAddsForServer_false() throws IOException {
    byte[] jsonBytes = "{ \"flags\":{} }".getBytes();
    Configuration config = Configuration.builder(jsonBytes, true).build();
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
            Collections.emptyList(),
            Collections.emptyList());

    // Create configuration with this flag
    Map<String, FlagConfig> flags = Collections.singletonMap("test-flag", flagConfig);
    Configuration config =
        new Configuration(flags, Collections.emptyMap(), Collections.emptyMap(), false, null, null);

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
            Collections.emptyList(),
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
            null,
            null);

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
}

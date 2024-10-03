package cloud.eppo.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cloud.eppo.ufc.dto.FlagConfigResponse;
import cloud.eppo.ufc.dto.adapters.EppoModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ConfigurationBuilderTest {

  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(EppoModule.eppoModule());

  @Test
  public void testHydrateConfigFromBytesForServer_true() {
    byte[] jsonBytes = "{ \"forServer\": true, \"flags\":{} }".getBytes();
    Configuration config = new Configuration.Builder(jsonBytes).build();
    assertFalse(config.isConfigObfuscated());
  }

  @Test
  public void testHydrateConfigFromBytesForServer_false() {
    byte[] jsonBytes = "{ \"forServer\": false, \"flags\":{} }".getBytes();
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

    assertTrue(rehydratedConfig.isForServer());
  }

  @Test
  public void testBuildConfigAddsForServer_false() throws IOException {
    byte[] jsonBytes = "{ \"flags\":{} }".getBytes();
    Configuration config = Configuration.builder(jsonBytes, true).build();
    assertTrue(config.isConfigObfuscated());

    byte[] serializedFlags = config.serializeFlagConfigToBytes();
    FlagConfigResponse rehydratedConfig =
        mapper.readValue(serializedFlags, FlagConfigResponse.class);

    assertFalse(rehydratedConfig.isForServer());
  }
}

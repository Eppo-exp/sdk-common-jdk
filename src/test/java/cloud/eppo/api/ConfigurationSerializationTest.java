package cloud.eppo.api;

import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.JacksonConfigurationParser;
import cloud.eppo.api.dto.BanditParametersResponse;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.parser.ConfigurationParser;
import java.io.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

/** Tests to verify that Configuration and its nested types can be serialized and deserialized. */
public class ConfigurationSerializationTest {

  private static final File flagsFile = new File("src/test/resources/shared/ufc/flags-v1.json");
  private static final File banditFlagsFile =
      new File("src/test/resources/shared/ufc/bandit-flags-v1.json");
  private static final File banditModelsFile =
      new File("src/test/resources/shared/ufc/bandit-models-v1.json");

  private static final ConfigurationParser<?> parser = new JacksonConfigurationParser();

  @Test
  public void testConfigurationSerializesAndDeserializes() throws Exception {
    // Load configuration from test resources
    byte[] flagsJson = FileUtils.readFileToByteArray(flagsFile);
    FlagConfigResponse flagConfigResponse = parser.parseFlagConfig(flagsJson);
    Configuration original = Configuration.builder(flagConfigResponse).build();

    // Serialize to bytes
    byte[] serialized = serializeToBytes(original);

    // Deserialize back
    Configuration deserialized = deserializeFromBytes(serialized);

    // Verify the deserialized configuration matches the original
    assertNotNull(deserialized);
    assertEquals(original.isConfigObfuscated(), deserialized.isConfigObfuscated());
    assertEquals(original.getEnvironmentName(), deserialized.getEnvironmentName());
    assertEquals(original.getFlagKeys(), deserialized.getFlagKeys());

    // Verify specific flags can be retrieved
    assertNotNull(deserialized.getFlag("numeric_flag"));
    assertNotNull(deserialized.getFlag("empty_flag"));
  }

  @Test
  public void testConfigurationWithBanditsSerializesAndDeserializes() throws Exception {
    // Load bandit configuration from test resources
    byte[] banditFlagsJson = FileUtils.readFileToByteArray(banditFlagsFile);
    byte[] banditModelsJson = FileUtils.readFileToByteArray(banditModelsFile);

    FlagConfigResponse flagConfigResponse = parser.parseFlagConfig(banditFlagsJson);
    BanditParametersResponse banditParametersResponse = parser.parseBanditParams(banditModelsJson);

    Configuration original =
        Configuration.builder(flagConfigResponse)
            .banditParameters(banditParametersResponse)
            .build();

    // Serialize to bytes
    byte[] serialized = serializeToBytes(original);

    // Deserialize back
    Configuration deserialized = deserializeFromBytes(serialized);

    // Verify the deserialized configuration matches the original
    assertNotNull(deserialized);
    assertEquals(original.isConfigObfuscated(), deserialized.isConfigObfuscated());
    assertEquals(original.getFlagKeys(), deserialized.getFlagKeys());

    // Verify bandit parameters are preserved
    assertNotNull(deserialized.getBanditParameters("cold_start_bandit"));
    assertEquals(
        original.getBanditParameters("cold_start_bandit").getModelVersion(),
        deserialized.getBanditParameters("cold_start_bandit").getModelVersion());
  }

  @Test
  public void testEmptyConfigurationSerializesAndDeserializes() throws Exception {
    Configuration original = Configuration.emptyConfig();

    // Serialize to bytes
    byte[] serialized = serializeToBytes(original);

    // Deserialize back
    Configuration deserialized = deserializeFromBytes(serialized);

    // Verify the deserialized configuration matches the original
    assertNotNull(deserialized);
    assertTrue(deserialized.isEmpty());
    assertEquals(original.isConfigObfuscated(), deserialized.isConfigObfuscated());
  }

  @Test
  public void testObfuscatedConfigurationSerializesAndDeserializes() throws Exception {
    // Load obfuscated configuration
    File obfuscatedFile = new File("src/test/resources/shared/ufc/flags-v1-obfuscated.json");
    byte[] flagsJson = FileUtils.readFileToByteArray(obfuscatedFile);
    FlagConfigResponse flagConfigResponse = parser.parseFlagConfig(flagsJson);
    Configuration original = Configuration.builder(flagConfigResponse).build();

    // Serialize to bytes
    byte[] serialized = serializeToBytes(original);

    // Deserialize back
    Configuration deserialized = deserializeFromBytes(serialized);

    // Verify the deserialized configuration matches the original
    assertNotNull(deserialized);
    assertTrue(deserialized.isConfigObfuscated());
    assertEquals(original.getFlagKeys(), deserialized.getFlagKeys());
  }

  private byte[] serializeToBytes(Configuration config) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(config);
    oos.close();
    return baos.toByteArray();
  }

  private Configuration deserializeFromBytes(byte[] bytes)
      throws IOException, ClassNotFoundException {
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    ObjectInputStream ois = new ObjectInputStream(bais);
    Configuration config = (Configuration) ois.readObject();
    ois.close();
    return config;
  }
}

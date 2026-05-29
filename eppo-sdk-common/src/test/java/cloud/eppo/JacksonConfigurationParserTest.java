package cloud.eppo;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.BanditParametersResponse;
import cloud.eppo.api.dto.FlagConfig;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.api.dto.VariationType;
import cloud.eppo.parser.ConfigurationParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JacksonConfigurationParserTest {
  private JacksonConfigurationParser parser;

  @BeforeEach
  public void setUp() {
    parser = new JacksonConfigurationParser();
  }

  @Test
  public void testParseFlagConfig() throws IOException {
    byte[] flagConfigJson = loadTestResource("shared/ufc/flags-v1.json");

    FlagConfigResponse response = parser.parseFlagConfig(flagConfigJson);

    assertNotNull(response);
    assertThat(response.getFlags()).isNotEmpty();
    assertThat(response.getFlags()).containsKey("kill-switch");
    assertThat(response.getFlags()).containsKey("numeric_flag");

    FlagConfig killSwitch = response.getFlags().get("kill-switch");
    assertEquals("kill-switch", killSwitch.getKey());
    assertTrue(killSwitch.isEnabled());
    assertEquals(VariationType.BOOLEAN, killSwitch.getVariationType());
    assertEquals("Test", response.getEnvironmentName());
    assertNotNull(response.getCreatedAt());
    assertEquals(FlagConfigResponse.Format.SERVER, response.getFormat());
  }

  @Test
  public void testParseBanditParams() throws IOException {
    byte[] banditParamsJson = loadTestResource("shared/ufc/bandit-models-v1.json");

    BanditParametersResponse banditsResponse = parser.parseBanditParams(banditParamsJson);

    assertNotNull(banditsResponse);
    assertNotNull(banditsResponse.getBandits());
    Map<String, BanditParameters> bandits = banditsResponse.getBandits();
    assertThat(bandits).containsKey("banner_bandit");

    BanditParameters bannerBandit = bandits.get("banner_bandit");
    assertEquals("banner_bandit", bannerBandit.getBanditKey());
    assertEquals("falcon", bannerBandit.getModelName());
    assertEquals("123", bannerBandit.getModelVersion());
    assertNotNull(bannerBandit.getUpdatedAt());
    assertNotNull(bannerBandit.getModelData());
    assertEquals(1.0, bannerBandit.getModelData().getGamma(), 0.001);
  }

  @Test
  public void testParseFlagConfigInvalidJson() {
    byte[] invalidJson = "not valid json".getBytes();

    ConfigurationParseException exception =
        assertThrows(ConfigurationParseException.class, () -> parser.parseFlagConfig(invalidJson));

    assertThat(exception.getMessage()).contains("Failed to parse flag configuration");
    assertNotNull(exception.getCause());
  }

  @Test
  public void testParseBanditParamsInvalidJson() {
    byte[] invalidJson = "{invalid}".getBytes();

    ConfigurationParseException exception =
        assertThrows(
            ConfigurationParseException.class, () -> parser.parseBanditParams(invalidJson));

    assertThat(exception.getMessage()).contains("Failed to parse bandit parameters");
    assertNotNull(exception.getCause());
  }

  @Test
  public void testParseFlagConfigEmptyFlags() {
    byte[] emptyFlagsJson = "{\"flags\": {}}".getBytes();

    FlagConfigResponse response = parser.parseFlagConfig(emptyFlagsJson);

    assertNotNull(response);
    assertThat(response.getFlags()).isEmpty();
  }

  @Test
  public void testParseBanditParamsEmptyBandits() {
    byte[] emptyBanditsJson = "{\"bandits\": {}}".getBytes();

    BanditParametersResponse banditsResponse = parser.parseBanditParams(emptyBanditsJson);
    Map<String, BanditParameters> bandits = banditsResponse.getBandits();

    assertNotNull(banditsResponse);
    assertNotNull(bandits);
    assertThat(bandits).isEmpty();
  }

  @Test
  public void testParseFlagConfigWithBanditReferences() throws IOException {
    byte[] flagConfigJson = loadTestResource("shared/ufc/bandit-flags-v1.json");

    FlagConfigResponse response = parser.parseFlagConfig(flagConfigJson);

    assertNotNull(response.getBanditReferences());
    assertThat(response.getBanditReferences()).isNotEmpty();
  }

  private byte[] loadTestResource(String relativePath) throws IOException {
    // Test resources are in the root project, so we need to go up from eppo-sdk-common
    Path path = Paths.get("../src/test/resources", relativePath);
    return Files.readAllBytes(path);
  }
}

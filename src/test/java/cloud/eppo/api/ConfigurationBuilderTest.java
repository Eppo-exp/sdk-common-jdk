package cloud.eppo.api;

import static cloud.eppo.Utils.getMD5Hex;
import static org.junit.jupiter.api.Assertions.*;

import cloud.eppo.api.dto.BanditModelData;
import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.BanditParametersResponse;
import cloud.eppo.api.dto.BanditReference;
import cloud.eppo.api.dto.FlagConfig;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.api.dto.VariationType;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Configuration} and {@link Configuration.Builder}: building from {@link
 * FlagConfigResponse}, bandit parameters, metadata, and Configuration behavior (lookups, empty
 * config). Uses DTOs directly so the focus is on Builder/Configuration logic, not the parser.
 */
public class ConfigurationBuilderTest {

  private static FlagConfigResponse emptyResponse(FlagConfigResponse.Format format) {
    return new FlagConfigResponse.Default(
        Collections.emptyMap(), Collections.emptyMap(), format, null, null);
  }

  private static FlagConfigResponse responseWithMetadata(
      String environmentName, Date createdAt, FlagConfigResponse.Format format) {
    return new FlagConfigResponse.Default(
        Collections.emptyMap(), Collections.emptyMap(), format, environmentName, createdAt);
  }

  @Test
  public void builderWithServerFormat_setsConfigNotObfuscated() {
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration config = new Configuration.Builder(response).build();
    assertFalse(config.isConfigObfuscated());
  }

  @Test
  public void builderWithClientFormat_setsConfigObfuscated() {
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.CLIENT);
    Configuration config = new Configuration.Builder(response).build();
    assertTrue(config.isConfigObfuscated());
  }

  @Test
  public void builderWithExplicitObfuscation_overridesResponseFormat() {
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration config = new Configuration.Builder(response, true).build();
    assertTrue(config.isConfigObfuscated());
  }

  @Test
  public void builderWithNullResponse_createsEmptyConfigWithNullMetadata() {
    Configuration config = new Configuration.Builder(null, false).build();
    assertTrue(config.getFlagKeys().isEmpty());
    assertNull(config.getEnvironmentName());
    assertNull(config.getConfigPublishedAt());
    assertFalse(config.isConfigObfuscated());
    assertNotNull(config.getConfigFetchedAt());
  }

  @Test
  public void builderCopiesEnvironmentNameFromResponse() {
    FlagConfigResponse response =
        responseWithMetadata("Production", null, FlagConfigResponse.Format.SERVER);
    Configuration config = new Configuration.Builder(response).build();
    assertEquals("Production", config.getEnvironmentName());
  }

  @Test
  public void builderCopiesConfigPublishedAtFromResponse() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date createdAt = sdf.parse("2024-04-17T19:40:53.716Z");
    FlagConfigResponse response =
        responseWithMetadata(null, createdAt, FlagConfigResponse.Format.SERVER);
    Configuration config = new Configuration.Builder(response).build();
    assertEquals(createdAt, config.getConfigPublishedAt());
  }

  @Test
  public void builderWithResponseWithoutMetadata_hasNullEnvironmentAndPublishedAt() {
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration config = new Configuration.Builder(response).build();
    assertNull(config.getEnvironmentName());
    assertNull(config.getConfigPublishedAt());
  }

  @Test
  public void build_setsConfigFetchedAtToCurrentTime() throws InterruptedException {
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Date beforeBuild = new Date();
    Thread.sleep(10);
    Configuration config = new Configuration.Builder(response).build();
    Thread.sleep(10);
    Date afterBuild = new Date();
    Date fetchedAt = config.getConfigFetchedAt();
    assertNotNull(fetchedAt);
    assertFalse(fetchedAt.before(beforeBuild));
    assertFalse(fetchedAt.after(afterBuild));
  }

  @Test
  public void banditParameters_nullResponse_leavesBanditsEmpty() {
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration config =
        new Configuration.Builder(response)
            .banditParameters((BanditParametersResponse) null)
            .build();
    assertNull(config.getBanditParameters("any-bandit"));
  }

  @Test
  public void banditParameters_responseWithNullBandits_leavesBanditsEmpty() {
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration config =
        new Configuration.Builder(response)
            .banditParameters(new BanditParametersResponse.Default(null))
            .build();
    assertNull(config.getBanditParameters("any-bandit"));
  }

  @Test
  public void banditParameters_responseWithBandits_populatesConfig() {
    BanditModelData modelData = new BanditModelData.Default(0.0, 1.0, 0.1, Collections.emptyMap());
    BanditParameters bandit1 =
        new BanditParameters.Default("bandit-1", new Date(), "falcon", "v1", modelData);
    BanditParameters bandit2 =
        new BanditParameters.Default("bandit-2", new Date(), "contextual-bandit", "v2", modelData);
    Map<String, BanditParameters> banditsMap = new HashMap<>();
    banditsMap.put("bandit-1", bandit1);
    banditsMap.put("bandit-2", bandit2);
    BanditParametersResponse banditResponse = new BanditParametersResponse.Default(banditsMap);

    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration config =
        new Configuration.Builder(response).banditParameters(banditResponse).build();

    assertEquals("falcon", config.getBanditParameters("bandit-1").getModelName());
    assertEquals("contextual-bandit", config.getBanditParameters("bandit-2").getModelName());
  }

  @Test
  public void banditParametersFromConfig_null_clearsBandits() {
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration.Builder builder = new Configuration.Builder(response);
    builder.banditParametersFromConfig(null);
    Configuration config = builder.build();
    assertNull(config.getBanditParameters("any"));
  }

  @Test
  public void banditParametersFromConfig_withConfig_copiesBanditsIntoBuiltConfig() {
    BanditModelData modelData = new BanditModelData.Default(0.0, 1.0, 0.1, Collections.emptyMap());
    BanditParameters bandit =
        new BanditParameters.Default("b", new Date(), "model", "v1", modelData);
    Map<String, BanditParameters> banditsMap = Collections.singletonMap("b", bandit);
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration existingConfig =
        new Configuration.Builder(response)
            .banditParameters(new BanditParametersResponse.Default(banditsMap))
            .build();

    Configuration config =
        new Configuration.Builder(response).banditParametersFromConfig(existingConfig).build();
    assertNotNull(config.getBanditParameters("b"));
    assertEquals("model", config.getBanditParameters("b").getModelName());
  }

  @Test
  public void flagsSnapshotId_setsSnapshotIdOnBuiltConfig() {
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration config = new Configuration.Builder(response).flagsSnapshotId("etag-123").build();
    assertEquals("etag-123", config.getFlagsSnapshotId());
  }

  @Test
  public void referencedBanditModelVersion_returnsVersionsFromResponse() {
    BanditReference ref = new BanditReference.Default("v2", Collections.emptyList());
    Map<String, BanditReference> refs = Collections.singletonMap("bandit-1", ref);
    FlagConfigResponse response =
        new FlagConfigResponse.Default(
            Collections.emptyMap(), refs, FlagConfigResponse.Format.SERVER, null, null);
    Configuration.Builder builder = new Configuration.Builder(response);
    assertTrue(builder.referencedBanditModelVersion().contains("v2"));
  }

  @Test
  public void loadedBanditModelVersions_returnsVersionsFromSetBandits() {
    BanditModelData modelData = new BanditModelData.Default(0.0, 1.0, 0.1, Collections.emptyMap());
    BanditParameters bandit = new BanditParameters.Default("b", new Date(), "m", "v3", modelData);
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration.Builder builder =
        new Configuration.Builder(response)
            .banditParameters(
                new BanditParametersResponse.Default(Collections.singletonMap("b", bandit)));
    assertTrue(builder.loadedBanditModelVersions().contains("v3"));
  }

  @Test
  public void requiresUpdatedBanditModels_whenReferencedVersionNotLoaded_returnsTrue() {
    BanditReference ref = new BanditReference.Default("v-needed", Collections.emptyList());
    Map<String, BanditReference> refs = Collections.singletonMap("bandit-1", ref);
    FlagConfigResponse response =
        new FlagConfigResponse.Default(
            Collections.emptyMap(), refs, FlagConfigResponse.Format.SERVER, null, null);
    Configuration.Builder builder = new Configuration.Builder(response);
    assertTrue(builder.requiresUpdatedBanditModels());
  }

  @Test
  public void requiresUpdatedBanditModels_whenReferencedVersionLoaded_returnsFalse() {
    BanditReference ref = new BanditReference.Default("v1", Collections.emptyList());
    Map<String, BanditReference> refs = Collections.singletonMap("bandit-1", ref);
    BanditModelData modelData = new BanditModelData.Default(0.0, 1.0, 0.1, Collections.emptyMap());
    BanditParameters bandit =
        new BanditParameters.Default("bandit-1", new Date(), "m", "v1", modelData);
    FlagConfigResponse response =
        new FlagConfigResponse.Default(
            Collections.emptyMap(), refs, FlagConfigResponse.Format.SERVER, null, null);
    Configuration.Builder builder =
        new Configuration.Builder(response)
            .banditParameters(
                new BanditParametersResponse.Default(Collections.singletonMap("bandit-1", bandit)));
    assertFalse(builder.requiresUpdatedBanditModels());
  }

  @Test
  public void getFlagType_returnsTypeWhenFlagPresent() {
    FlagConfig flagConfig =
        new FlagConfig.Default(
            "test-flag",
            true,
            1,
            VariationType.STRING,
            Collections.emptyMap(),
            Collections.emptyList());
    Map<String, FlagConfig> flags = Collections.singletonMap("test-flag", flagConfig);
    Configuration config =
        new Configuration(
            flags, Collections.emptyMap(), Collections.emptyMap(), false, null, null, null, null);
    assertEquals(VariationType.STRING, config.getFlagType("test-flag"));
    assertNull(config.getFlagType("non-existent-flag"));
  }

  @Test
  public void getFlagType_withObfuscatedConfig_usesHashedLookup() {
    FlagConfig flagConfig =
        new FlagConfig.Default(
            "test-flag",
            true,
            1,
            VariationType.NUMERIC,
            Collections.emptyMap(),
            Collections.emptyList());
    String hashedKey = getMD5Hex("test-flag");
    Map<String, FlagConfig> flags = Collections.singletonMap(hashedKey, flagConfig);
    Configuration config =
        new Configuration(
            flags, Collections.emptyMap(), Collections.emptyMap(), true, null, null, null, null);
    assertEquals(VariationType.NUMERIC, config.getFlagType("test-flag"));
    assertNull(config.getFlagType("non-existent-flag"));
    assertNull(config.getFlagType(hashedKey));
  }

  @Test
  public void getFlagType_emptyConfig_returnsNull() {
    Configuration config = Configuration.emptyConfig();
    assertNull(config.getFlagType("any-flag"));
  }

  @Test
  public void emptyConfig_hasNullMetadata() {
    Configuration config = Configuration.emptyConfig();
    assertNull(config.getEnvironmentName());
    assertNull(config.getConfigFetchedAt());
    assertNull(config.getConfigPublishedAt());
  }

  @Test
  public void builderStaticFactory_equivalentToConstructor() {
    FlagConfigResponse response = emptyResponse(FlagConfigResponse.Format.SERVER);
    Configuration fromConstructor = new Configuration.Builder(response).build();
    Configuration fromFactory = Configuration.builder(response).build();
    assertEquals(fromConstructor.isConfigObfuscated(), fromFactory.isConfigObfuscated());
    assertEquals(fromConstructor.getFlagKeys(), fromFactory.getFlagKeys());
  }
}

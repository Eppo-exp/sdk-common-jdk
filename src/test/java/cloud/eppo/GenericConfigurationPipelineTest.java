package cloud.eppo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cloud.eppo.api.SerializableEppoConfiguration;
import cloud.eppo.api.dto.BanditParameters;
import cloud.eppo.api.dto.BanditReference;
import cloud.eppo.api.dto.FlagConfig;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.api.dto.VariationType;
import cloud.eppo.http.EppoConfigurationClient;
import cloud.eppo.http.EppoConfigurationRequest;
import cloud.eppo.http.EppoConfigurationRequestFactory;
import cloud.eppo.http.EppoConfigurationResponse;
import cloud.eppo.parser.ConfigurationParseException;
import cloud.eppo.parser.ConfigurationParser;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the generic configuration pipeline end-to-end using a stub ConfigurationType that is NOT
 * Configuration. This ensures the 2-param generic design works for custom implementations.
 */
public class GenericConfigurationPipelineTest {

  // ---------------------------------------------------------------------------
  // Stub ConfigurationType — does not use Configuration or JacksonConfigurationParser
  // ---------------------------------------------------------------------------

  static class StubConfig implements SerializableEppoConfiguration {
    private static final long serialVersionUID = 1L;

    final String source;
    final boolean banditsApplied;
    final String snapshotId;

    StubConfig(String source, boolean banditsApplied, String snapshotId) {
      this.source = source;
      this.banditsApplied = banditsApplied;
      this.snapshotId = snapshotId;
    }

    @Override
    public FlagConfig getFlag(String flagKey) {
      return null;
    }

    @Override
    public @Nullable VariationType getFlagType(String flagKey) {
      return null;
    }

    @Override
    public String getEnvironmentName() {
      return "stub-env";
    }

    @Override
    public Date getConfigFetchedAt() {
      return new Date();
    }

    @Override
    public Date getConfigPublishedAt() {
      return null;
    }

    @Override
    public boolean isConfigObfuscated() {
      return false;
    }

    @Override
    public String banditKeyForVariation(String flagKey, String variationValue) {
      return null;
    }

    @Override
    public BanditParameters getBanditParameters(String banditKey) {
      return null;
    }

    @Override
    public boolean isEmpty() {
      return "empty".equals(source);
    }

    @Override
    public String getFlagsSnapshotId() {
      return snapshotId;
    }

    @Override
    public Set<String> getFlagKeys() {
      return Collections.emptySet();
    }
  }

  // ---------------------------------------------------------------------------
  // Stub ConfigurationParser
  // ---------------------------------------------------------------------------

  static class StubParser implements ConfigurationParser<StubConfig, String> {
    final AtomicInteger buildConfigCallCount = new AtomicInteger(0);
    final AtomicInteger parseFlagConfigCallCount = new AtomicInteger(0);

    // Controls what bandit references the parsed flag response will report.
    Map<String, BanditReference> banditReferences = Collections.emptyMap();

    @Override
    @NotNull public FlagConfigResponse parseFlagConfig(@NotNull byte[] flagConfigBytes)
        throws ConfigurationParseException {
      parseFlagConfigCallCount.incrementAndGet();
      String bytesStr = new String(flagConfigBytes);
      return new FlagConfigResponse.Default(null, banditReferences) {
        @Override
        public String toString() {
          return "StubFlagConfigResponse[" + bytesStr + "]";
        }
      };
    }

    @Override
    @NotNull public StubConfig buildConfig(
        @NotNull FlagConfigResponse flags,
        @Nullable String flagsSnapshotId,
        @Nullable StubConfig previousConfig,
        @Nullable byte[] banditParamsBytes) {
      buildConfigCallCount.incrementAndGet();
      boolean banditsApplied = banditParamsBytes != null;
      // Recover the original "source" string from the flag response toString
      String source = flags.toString();
      if (source.startsWith("StubFlagConfigResponse[") && source.endsWith("]")) {
        source = source.substring("StubFlagConfigResponse[".length(), source.length() - 1);
      }
      return new StubConfig(source, banditsApplied, flagsSnapshotId);
    }

    @Override
    @NotNull public String parseJsonValue(@NotNull String jsonValue) throws ConfigurationParseException {
      return jsonValue;
    }
  }

  // ---------------------------------------------------------------------------
  // Test setup
  // ---------------------------------------------------------------------------

  /** Minimal in-memory store for any SerializableEppoConfiguration subtype. */
  static class SimpleConfigStore<T extends SerializableEppoConfiguration>
      implements IConfigurationStore<T> {
    private volatile T config;

    SimpleConfigStore(T initial) {
      this.config = initial;
    }

    @Override
    public @NotNull T getConfiguration() {
      return config;
    }

    @Override
    public CompletableFuture<Void> saveConfiguration(T configuration) {
      this.config = configuration;
      return CompletableFuture.completedFuture(null);
    }

    // No-op: this pipeline test only verifies fetch/parse/save, not subscription behavior.
    @Override
    public Runnable subscribe(Consumer<T> callback) {
      return () -> {};
    }

    @Override
    public boolean unsubscribe(Consumer<T> callback) {
      return false;
    }
  }

  private IConfigurationStore<StubConfig> configStore;
  private EppoConfigurationClient mockConfigClient;
  private StubParser stubParser;
  private EppoConfigurationRequestFactory requestFactory;

  @BeforeEach
  void setUp() {
    configStore = new SimpleConfigStore<>(new StubConfig("empty", false, null));
    mockConfigClient = mock(EppoConfigurationClient.class);
    stubParser = new StubParser();
    requestFactory =
        new EppoConfigurationRequestFactory(
            "https://test.eppo.cloud", "test-api-key", "java", "1.0.0");
  }

  private ConfigurationRequestor<StubConfig, String> createRequestor(boolean supportBandits) {
    return new ConfigurationRequestor<>(
        configStore, supportBandits, stubParser, mockConfigClient, requestFactory);
  }

  private void stubSuccessResponse(String body, String versionId) {
    EppoConfigurationResponse response =
        EppoConfigurationResponse.success(200, versionId, body.getBytes());
    when(mockConfigClient.execute(any(EppoConfigurationRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(response));
  }

  // ---------------------------------------------------------------------------
  // Tests
  // ---------------------------------------------------------------------------

  @Test
  void testBuildConfigIsCalledOnFetch() {
    stubSuccessResponse("stub-flags-body", "etag-1");
    ConfigurationRequestor<StubConfig, String> requestor = createRequestor(false);

    requestor.fetchAndSaveFromRemote();

    assertEquals(1, stubParser.buildConfigCallCount.get());
    StubConfig saved = configStore.getConfiguration();
    assertNotNull(saved);
    assertEquals("stub-flags-body", saved.source);
    assertEquals("etag-1", saved.snapshotId);
    assertFalse(saved.banditsApplied);
  }

  @Test
  void testSnapshotIdPassedThroughToBuildConfig() {
    stubSuccessResponse("{}", "my-etag-v2");
    ConfigurationRequestor<StubConfig, String> requestor = createRequestor(false);

    requestor.fetchAndSaveFromRemote();

    assertEquals("my-etag-v2", configStore.getConfiguration().snapshotId);
  }

  @Test
  void testPreviousConfigPassedToBuildConfig_OnSubsequentFetch() {
    // Wrap parser to capture previousConfig argument
    AtomicInteger secondCallPreviousConfigCount = new AtomicInteger(0);
    StubParser trackingParser =
        new StubParser() {
          @Override
          @NotNull public StubConfig buildConfig(
              @NotNull FlagConfigResponse flags,
              @Nullable String flagsSnapshotId,
              @Nullable StubConfig previousConfig,
              @Nullable byte[] banditParamsBytes) {
            StubConfig result =
                super.buildConfig(flags, flagsSnapshotId, previousConfig, banditParamsBytes);
            if (previousConfig != null && "first-body".equals(previousConfig.source)) {
              secondCallPreviousConfigCount.incrementAndGet();
            }
            return result;
          }
        };

    ConfigurationRequestor<StubConfig, String> requestor =
        new ConfigurationRequestor<>(
            configStore, false, trackingParser, mockConfigClient, requestFactory);

    EppoConfigurationResponse first =
        EppoConfigurationResponse.success(200, "v1", "first-body".getBytes());
    EppoConfigurationResponse second =
        EppoConfigurationResponse.success(200, "v2", "second-body".getBytes());

    when(mockConfigClient.execute(any(EppoConfigurationRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(first))
        .thenReturn(CompletableFuture.completedFuture(second));

    requestor.fetchAndSaveFromRemote();
    requestor.fetchAndSaveFromRemote();

    assertEquals(
        1,
        secondCallPreviousConfigCount.get(),
        "Previous config from first fetch should be passed to second buildConfig call");
  }

  @Test
  void testNoBanditFetchWhenNoBanditReferences() {
    // No bandit references in the parsed response → no bandit fetch
    stubSuccessResponse("{}", "v1");
    stubParser.banditReferences = Collections.emptyMap();
    ConfigurationRequestor<StubConfig, String> requestor = createRequestor(true);

    requestor.fetchAndSaveFromRemote();

    // Config client should only be called once (for flags, not bandits)
    verify(mockConfigClient, times(1)).execute(any());
    assertFalse(configStore.getConfiguration().banditsApplied);
  }

  @Test
  void testBanditFetchAndApplyWhenBanditReferencePresent() {
    EppoConfigurationResponse flagResponse =
        EppoConfigurationResponse.success(200, "v1", "flag-body".getBytes());
    EppoConfigurationResponse banditResponse =
        EppoConfigurationResponse.success(200, null, "bandit-body".getBytes());

    when(mockConfigClient.execute(any(EppoConfigurationRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(flagResponse))
        .thenReturn(CompletableFuture.completedFuture(banditResponse));

    // Simulate a flag response that references a bandit model not yet loaded
    BanditReference ref = new BanditReference.Default("v1", Collections.emptyList());
    stubParser.banditReferences = Collections.singletonMap("test-bandit", ref);
    ConfigurationRequestor<StubConfig, String> requestor = createRequestor(true);

    requestor.fetchAndSaveFromRemote();

    // Two HTTP calls: one for flags, one for bandits
    verify(mockConfigClient, times(2)).execute(any());
    assertTrue(configStore.getConfiguration().banditsApplied);
  }

  @Test
  void testNoBanditEvenIfReferencedWhenSupportBanditsIsFalse() {
    stubSuccessResponse("{}", "v1");
    BanditReference ref = new BanditReference.Default("v1", Collections.emptyList());
    stubParser.banditReferences = Collections.singletonMap("test-bandit", ref);
    ConfigurationRequestor<StubConfig, String> requestor = createRequestor(false);

    requestor.fetchAndSaveFromRemote();

    // supportBandits=false → no bandit fetch regardless of references
    verify(mockConfigClient, times(1)).execute(any());
    assertFalse(configStore.getConfiguration().banditsApplied);
  }

  @Test
  void testAsyncFetchCallsBuildConfig() {
    stubSuccessResponse("async-body", "async-etag");
    ConfigurationRequestor<StubConfig, String> requestor = createRequestor(false);

    requestor.fetchAndSaveFromRemoteAsync().join();

    assertEquals(1, stubParser.buildConfigCallCount.get());
    assertEquals("async-body", configStore.getConfiguration().source);
    assertEquals("async-etag", configStore.getConfiguration().snapshotId);
  }
}

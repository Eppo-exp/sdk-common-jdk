# Eppo SDK v4.0 Migration Guide

This guide documents all breaking changes when upgrading from v3.x to v4.0. It is designed for both human developers and coding agents performing automated migrations.

**Last Updated:** Generated from snapshot/v4 branch comparison against main

---

## Table of Contents

1. [Artifacts Overview](#artifacts-overview)
2. [Package Relocations](#package-relocations)
3. [DTOs Converted to Interfaces](#dtos-converted-to-interfaces)
4. [Removed Classes](#removed-classes)
5. [New Pluggable Interfaces](#new-pluggable-interfaces)
6. [Configuration.Builder Changes](#configurationbuilder-changes)
7. [BaseEppoClient Changes](#baseeppoclient-changes)
8. [EppoValue.unwrap() Changes](#eppovalueunwrap-changes)
9. [HTTP Abstraction Layer](#http-abstraction-layer)
10. [Utils.Base64Codec](#utilsbase64codec)
11. [Quick Migration Checklist](#quick-migration-checklist)
12. [Complete Examples](#complete-examples)

---

## Artifacts Overview

### v3.x Structure
- Single artifact: `cloud.eppo:eppo-sdk-framework` (used for all implementations)
- All HTTP and parsing logic embedded in framework

### v4.0 Structure

Two artifacts with different purposes:

| Artifact | Version | Description | When to Use |
|----------|---------|-------------|-------------|
| `cloud.eppo:sdk-common-jvm` | `4.0.0` | **Batteries-included SDK** with OkHttp client and Jackson parser | Most users - ready to use out of box |
| `cloud.eppo:eppo-sdk-framework` | `0.1.0` | **Core framework** with interfaces only | Custom implementations (different JSON library, custom networking) |

**Recommendation:** Use `sdk-common-jvm` unless you have specific requirements for custom HTTP or JSON handling.

---

## Package Relocations

All DTO classes moved from `cloud.eppo.ufc.dto` to `cloud.eppo.api.dto`.

### Find and Replace

```bash
# Automated replacement
sed -i 's/cloud\.eppo\.ufc\.dto/cloud.eppo.api.dto/g' **/*.java **/*.kt
```

### Affected Classes

All of these classes require import updates:

```
cloud.eppo.ufc.dto.Allocation                           → cloud.eppo.api.dto.Allocation
cloud.eppo.ufc.dto.BanditAttributeCoefficients          → cloud.eppo.api.dto.BanditAttributeCoefficients
cloud.eppo.ufc.dto.BanditCategoricalAttributeCoefficients → cloud.eppo.api.dto.BanditCategoricalAttributeCoefficients
cloud.eppo.ufc.dto.BanditCoefficients                   → cloud.eppo.api.dto.BanditCoefficients
cloud.eppo.ufc.dto.BanditFlagVariation                  → cloud.eppo.api.dto.BanditFlagVariation
cloud.eppo.ufc.dto.BanditModelData                      → cloud.eppo.api.dto.BanditModelData
cloud.eppo.ufc.dto.BanditNumericAttributeCoefficients   → cloud.eppo.api.dto.BanditNumericAttributeCoefficients
cloud.eppo.ufc.dto.BanditParameters                     → cloud.eppo.api.dto.BanditParameters
cloud.eppo.ufc.dto.BanditParametersResponse             → cloud.eppo.api.dto.BanditParametersResponse
cloud.eppo.ufc.dto.BanditReference                      → cloud.eppo.api.dto.BanditReference
cloud.eppo.ufc.dto.EppoValueType                        → cloud.eppo.api.dto.EppoValueType
cloud.eppo.ufc.dto.FlagConfig                           → cloud.eppo.api.dto.FlagConfig
cloud.eppo.ufc.dto.FlagConfigResponse                   → cloud.eppo.api.dto.FlagConfigResponse
cloud.eppo.ufc.dto.OperatorType                         → cloud.eppo.api.dto.OperatorType
cloud.eppo.ufc.dto.Shard                                → cloud.eppo.api.dto.Shard
cloud.eppo.ufc.dto.Split                                → cloud.eppo.api.dto.Split
cloud.eppo.ufc.dto.TargetingCondition                   → cloud.eppo.api.dto.TargetingCondition
cloud.eppo.ufc.dto.TargetingRule                        → cloud.eppo.api.dto.TargetingRule
cloud.eppo.ufc.dto.Variation                            → cloud.eppo.api.dto.Variation
cloud.eppo.ufc.dto.VariationType                        → cloud.eppo.api.dto.VariationType
```

---

## DTOs Converted to Interfaces

Major DTOs converted from concrete classes to interfaces with nested `Default` implementations.

### Affected Types

All of these are now interfaces:
- `Allocation`
- `BanditAttributeCoefficients`
- `BanditCategoricalAttributeCoefficients`
- `BanditCoefficients`
- `BanditFlagVariation`
- `BanditModelData`
- `BanditNumericAttributeCoefficients`
- `BanditParameters`
- `BanditParametersResponse`
- `BanditReference`
- `FlagConfig`
- `FlagConfigResponse`
- `Shard`
- `Split`
- `TargetingCondition`
- `TargetingRule`
- `Variation`

### Migration Pattern

```java
// v3.x - Direct instantiation
FlagConfig config = new FlagConfig(key, enabled, shards, type, variations, allocations);
FlagConfigResponse response = new FlagConfigResponse(flags, refs, format, env, created);

// v4.0 - Use nested Default class
FlagConfig config = new FlagConfig.Default(key, enabled, shards, type, variations, allocations);
FlagConfigResponse response = new FlagConfigResponse.Default(flags, refs, format, env, created);
```

**Note:** If you only use these types as method parameters or return types (not direct instantiation), no code changes are needed.

---

## Removed Classes

### `cloud.eppo.EppoHttpClient` - REMOVED

The built-in HTTP client has been removed from the framework.

**Migration:**
- **If using `sdk-common-jvm`:** No action needed - `OkHttpEppoClient` is provided
- **If using framework only:** Implement `EppoConfigurationClient` interface

### `cloud.eppo.EppoHttpClientRequestCallback` - REMOVED

Callback interface replaced by `CompletableFuture` pattern in new `EppoConfigurationClient`.

### Removed Methods

| Class | Removed Method | Replacement |
|-------|----------------|-------------|
| `Configuration` | `serializeFlagConfigToBytes()` | Store parsed DTOs or use parser directly |
| `Configuration` | `serializeBanditParamsToBytes()` | Store parsed DTOs or use parser directly |
| `Attributes` | `serializeNonNullAttributesToJSONString()` | Implement your own serialization |

---

## New Pluggable Interfaces

### `ConfigurationParser<JSONFlagType>`

Interface for parsing configuration JSON. Decouples JSON library choice from framework.

```java
public interface ConfigurationParser<JSONFlagType> {
  FlagConfigResponse parseFlagConfig(byte[] flagConfigJson) throws ConfigurationParseException;
  BanditParametersResponse parseBanditParams(byte[] banditParamsJson) throws ConfigurationParseException;
  JSONFlagType parseJsonValue(String jsonValue) throws ConfigurationParseException;
}
```

**Default implementation (sdk-common-jvm):** `JacksonConfigurationParser`

**Usage:**
```java
// Using the default Jackson implementation
ConfigurationParser<JsonNode> parser = new JacksonConfigurationParser();
FlagConfigResponse flagConfig = parser.parseFlagConfig(jsonBytes);
```

### `EppoConfigurationClient`

Interface for HTTP operations. Allows custom HTTP implementations.

```java
public interface EppoConfigurationClient {
  CompletableFuture<EppoConfigurationResponse> execute(EppoConfigurationRequest request);
}
```

**Default implementation (sdk-common-jvm):** `OkHttpEppoClient`

**Usage:**
```java
// Using the default OkHttp implementation
EppoConfigurationClient client = new OkHttpEppoClient();
```

### When Do You Need These?

| Artifact Used | Need to Implement? |
|---------------|-------------------|
| `sdk-common-jvm` | ❌ No - defaults provided |
| `eppo-sdk-framework` | ✅ Yes - must implement both |

---

## Configuration.Builder Changes

The builder no longer accepts raw JSON bytes. It now requires pre-parsed objects.

### v3.x API

```java
// Takes raw JSON bytes
Configuration config = new Configuration.Builder(flagConfigJsonBytes)
    .banditParameters(banditParamsJsonBytes)  // byte[] or String
    .build();
```

### v4.0 API

```java
// Takes parsed objects
ConfigurationParser<JsonNode> parser = new JacksonConfigurationParser();
FlagConfigResponse flagConfig = parser.parseFlagConfig(flagConfigJsonBytes);
BanditParametersResponse banditParams = parser.parseBanditParams(banditParamsJsonBytes);

Configuration config = new Configuration.Builder(flagConfig)
    .banditParameters(banditParams)
    .flagsSnapshotId(versionId)  // NEW: for caching/ETags
    .build();
```

### Method Changes

| v3.x | v4.0 | Notes |
|------|------|-------|
| `Builder(byte[] flagJson)` | `Builder(FlagConfigResponse)` | Must parse first |
| `banditParameters(byte[] json)` | `banditParameters(BanditParametersResponse)` | Must parse first |
| `banditParameters(String json)` | `banditParameters(BanditParametersResponse)` | Must parse first |
| `requiresBanditModels()` | `requiresUpdatedBanditModels()` | Renamed |
| N/A | `flagsSnapshotId(String)` | NEW: ETag support |

### New Feature: Snapshot IDs

Configuration now tracks version IDs (ETags) for conditional requests:

```java
Configuration config = builder.flagsSnapshotId(response.getVersionId()).build();
String snapshotId = config.getFlagsSnapshotId();  // For subsequent requests
```

---

## BaseEppoClient Changes

The client is now generic and requires two new dependencies.

### Class Declaration

```java
// v3.x
public class MyClient extends BaseEppoClient { }

// v4.0 - Add type parameter for JSON library
public class MyClient extends BaseEppoClient<JsonNode> { }  // Jackson
public class MyClient extends BaseEppoClient<JsonElement> { }  // Gson
```

### Constructor Changes

```java
// v3.x - 13 parameters
protected BaseEppoClient(
    String apiKey,
    String sdkName,
    String sdkVersion,
    String apiBaseUrl,
    AssignmentLogger assignmentLogger,
    BanditLogger banditLogger,
    IConfigurationStore configurationStore,
    boolean isGracefulMode,
    boolean expectObfuscatedConfig,
    boolean supportBandits,
    CompletableFuture<Configuration> initialConfiguration,
    IAssignmentCache assignmentCache,
    IAssignmentCache banditAssignmentCache)

// v4.0 - 15 parameters (added 2)
protected BaseEppoClient<JsonFlagType>(
    String apiKey,
    String sdkName,
    String sdkVersion,
    String apiBaseUrl,
    AssignmentLogger assignmentLogger,
    BanditLogger banditLogger,
    IConfigurationStore configurationStore,
    boolean isGracefulMode,
    boolean expectObfuscatedConfig,
    boolean supportBandits,
    CompletableFuture<Configuration> initialConfiguration,
    IAssignmentCache assignmentCache,
    IAssignmentCache banditAssignmentCache,
    ConfigurationParser<JsonFlagType> configurationParser,  // NEW
    EppoConfigurationClient configurationClient)            // NEW
```

### JSON Assignment Methods

Return type changed from `JsonNode` to generic `JsonFlagType`:

```java
// v3.x
JsonNode getJSONAssignment(String flagKey, String subjectKey, JsonNode defaultValue);

// v4.0
JsonFlagType getJSONAssignment(String flagKey, String subjectKey, JsonFlagType defaultValue);
```

### Migration Example

```java
// v3.x
public class MyClient extends BaseEppoClient {
    public MyClient(String apiKey) {
        super(apiKey, "my-sdk", "1.0", null, null, null, null, false, false, true, null, null, null);
    }
}

// v4.0
public class MyClient extends BaseEppoClient<JsonNode> {
    public MyClient(String apiKey) {
        super(
            apiKey, "my-sdk", "1.0",
            null, null, null, null,
            false, false, true, null, null, null,
            new JacksonConfigurationParser(),  // NEW
            new OkHttpEppoClient()              // NEW
        );
    }
}
```

---

## EppoValue.unwrap() Changes

JSON unwrapping now requires an explicit parser function.

### v3.x API

```java
// All types including JSON worked with single method
Boolean bool = eppoValue.unwrap(VariationType.BOOLEAN);
String str = eppoValue.unwrap(VariationType.STRING);
JsonNode json = eppoValue.unwrap(VariationType.JSON);  // Used Jackson internally
```

### v4.0 API

```java
// Non-JSON types unchanged
Boolean bool = eppoValue.unwrap(VariationType.BOOLEAN);
Integer num = eppoValue.unwrap(VariationType.INTEGER);
Double dbl = eppoValue.unwrap(VariationType.NUMERIC);
String str = eppoValue.unwrap(VariationType.STRING);

// JSON requires parser function
JsonNode json = eppoValue.unwrap(VariationType.JSON, parser::parseJsonValue);

// Or with lambda
JsonNode json = eppoValue.unwrap(VariationType.JSON, jsonStr -> {
    try {
        return new ObjectMapper().readTree(jsonStr);
    } catch (Exception e) {
        return null;
    }
});

// Calling without parser throws exception
eppoValue.unwrap(VariationType.JSON);  // ❌ IllegalArgumentException
```

### Method Signatures

```java
// Existing method - throws for JSON type
public <T> T unwrap(VariationType expectedType)

// New overload - required for JSON
public <T> T unwrap(VariationType expectedType, Function<String, ?> jsonParser)
```

---

## HTTP Abstraction Layer

New package `cloud.eppo.http` provides clean HTTP abstractions.

### Core Classes

| Class | Purpose |
|-------|---------|
| `EppoConfigurationClient` | Interface for HTTP operations (single `execute()` method) |
| `EppoConfigurationRequest` | Immutable request (URL, params, version ID, method, body) |
| `EppoConfigurationResponse` | Immutable response (status, version ID, body) |
| `EppoConfigurationRequestFactory` | Creates typed requests for flags/bandits |

### Request/Response Flow

```java
// 1. Create factory with SDK info
EppoConfigurationRequestFactory factory = new EppoConfigurationRequestFactory(
    baseUrl, apiKey, sdkName, sdkVersion);

// 2. Create request (with conditional fetch support)
EppoConfigurationRequest request = factory.createFlagConfigRequest(lastVersionId);

// 3. Execute via client
EppoConfigurationClient client = new OkHttpEppoClient();
CompletableFuture<EppoConfigurationResponse> future = client.execute(request);

// 4. Handle response
EppoConfigurationResponse response = future.get();
if (response.isSuccessful()) {
    byte[] body = response.getBody();
    String newVersionId = response.getVersionId();  // Save for next request
} else if (response.isNotModified()) {
    // 304 Not Modified - use cached config
}
```

### Conditional Requests (304 Not Modified)

v4.0 supports efficient caching via ETags:

```java
// First request
String lastVersionId = null;
EppoConfigurationRequest req1 = factory.createFlagConfigRequest(lastVersionId);
EppoConfigurationResponse res1 = client.execute(req1).get();
lastVersionId = res1.getVersionId();  // Save ETag

// Subsequent request - may return 304
EppoConfigurationRequest req2 = factory.createFlagConfigRequest(lastVersionId);
EppoConfigurationResponse res2 = client.execute(req2).get();
if (res2.isNotModified()) {
    // No changes, use cached configuration
}
```

---

## Utils.Base64Codec

New pluggable interface for Base64 operations (useful for Android).

```java
public interface Base64Codec {
    String base64Encode(String input);
    String base64Decode(String input);
}
```

### Default Behavior

Uses `java.util.Base64` by default.

### Custom Implementation

```java
// Set custom codec (e.g., android.util.Base64)
Utils.setBase64Codec(new Utils.Base64Codec() {
    @Override
    public String base64Encode(String input) {
        return android.util.Base64.encodeToString(
            input.getBytes(StandardCharsets.UTF_8),
            android.util.Base64.NO_WRAP);
    }

    @Override
    public String base64Decode(String input) {
        return new String(
            android.util.Base64.decode(input, android.util.Base64.NO_WRAP),
            StandardCharsets.UTF_8);
    }
});
```

---

## Quick Migration Checklist

### All Users

- [ ] Update dependency: `sdk-common-jvm:4.0.0` (replaces `eppo-sdk-framework`)
- [ ] Update imports: `cloud.eppo.ufc.dto.*` → `cloud.eppo.api.dto.*`
- [ ] Replace DTO instantiation: `new FlagConfig(...)` → `new FlagConfig.Default(...)`
- [ ] Update Configuration.Builder: pass parsed `FlagConfigResponse` instead of `byte[]`
- [ ] Update JSON unwrap: `unwrap(JSON)` → `unwrap(JSON, parser::parseJsonValue)`
- [ ] Remove calls to removed methods: `serializeFlagConfigToBytes()`, etc.

### If Extending BaseEppoClient

- [ ] Add generic type parameter: `BaseEppoClient<JsonNode>`
- [ ] Add `ConfigurationParser` to constructor
- [ ] Add `EppoConfigurationClient` to constructor

### If Using Framework Only (Advanced)

- [ ] Implement `ConfigurationParser<YourJsonType>`
- [ ] Implement `EppoConfigurationClient`

---

## Complete Examples

### Basic Usage (sdk-common-jvm)

```java
// v3.x
import cloud.eppo.ufc.dto.FlagConfig;
import cloud.eppo.api.Configuration;

byte[] jsonBytes = fetchConfigJson();
Configuration config = new Configuration.Builder(jsonBytes).build();

// v4.0
import cloud.eppo.api.dto.FlagConfig;
import cloud.eppo.api.dto.FlagConfigResponse;
import cloud.eppo.api.Configuration;
import cloud.eppo.JacksonConfigurationParser;
import cloud.eppo.parser.ConfigurationParser;
import com.fasterxml.jackson.databind.JsonNode;

ConfigurationParser<JsonNode> parser = new JacksonConfigurationParser();
byte[] jsonBytes = fetchConfigJson();
FlagConfigResponse flagConfig = parser.parseFlagConfig(jsonBytes);
Configuration config = new Configuration.Builder(flagConfig).build();
```

### Client Implementation

```java
// v3.x
import cloud.eppo.BaseEppoClient;
import cloud.eppo.ufc.dto.VariationType;

public class MyEppoClient extends BaseEppoClient {
    public MyEppoClient(String apiKey) {
        super(apiKey, "my-sdk", "1.0.0", null, null, null, null, false, false, true, null, null, null);
    }
}

// v4.0
import cloud.eppo.BaseEppoClient;
import cloud.eppo.JacksonConfigurationParser;
import cloud.eppo.OkHttpEppoClient;
import cloud.eppo.api.dto.VariationType;
import com.fasterxml.jackson.databind.JsonNode;

public class MyEppoClient extends BaseEppoClient<JsonNode> {
    public MyEppoClient(String apiKey) {
        super(
            apiKey, "my-sdk", "1.0.0",
            null, null, null, null, false, false, true, null, null, null,
            new JacksonConfigurationParser(),
            new OkHttpEppoClient()
        );
    }
}
```

### JSON Flag Handling

```java
// v3.x
JsonNode json = eppoValue.unwrap(VariationType.JSON);

// v4.0
ConfigurationParser<JsonNode> parser = new JacksonConfigurationParser();
JsonNode json = eppoValue.unwrap(VariationType.JSON, parser::parseJsonValue);
```

---

## Dependency Configuration

### Using sdk-common-jvm (Recommended)

```groovy
// build.gradle
dependencies {
    implementation 'cloud.eppo:sdk-common-jvm:4.0.0'
}

repositories {
    mavenCentral()
}
```

Includes:
- `JacksonConfigurationParser` (Jackson-based)
- `OkHttpEppoClient` (OkHttp-based)
- All transitive dependencies
- **No custom implementations needed**

### Using Framework Only (Advanced)

```groovy
// build.gradle
dependencies {
    implementation 'cloud.eppo:eppo-sdk-framework:0.1.0'
}

repositories {
    mavenCentral()
}
```

You must implement:
- `ConfigurationParser<YourJsonType>`
- `EppoConfigurationClient`

---

## Architecture Benefits

v4.0 provides significant architectural improvements:

1. **Separation of Concerns:** Core framework separate from HTTP/parsing implementations
2. **Reduced Dependencies:** Framework no longer depends on Jackson or OkHttp
3. **Pluggable Architecture:** Easy to swap HTTP clients or JSON libraries
4. **Better Testing:** Interfaces make mocking and testing easier
5. **Caching Support:** Built-in ETag/conditional request support
6. **Platform Flexibility:** Easier to adapt for Android, GraalVM, etc.

---

## Need Help?

- **Documentation:** See `FRAMEWORK_SDK_GUIDE.md` for implementing custom parsers/clients
- **Issues:** https://github.com/Eppo-exp/sdk-common-jdk/issues
- **Examples:** Check `eppo-sdk-common` module for reference implementations

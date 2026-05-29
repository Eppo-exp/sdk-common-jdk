# Eppo SDK Framework Guide

This guide explains how to use the `eppo-sdk-framework` artifact to implement custom HTTP clients and configuration parsers.

**Use this guide if:**
- You need a custom JSON library (e.g., Gson, Moshi instead of Jackson)
- You have custom networking requirements (custom SSL, proxies, authentication)
- You're on a platform where OkHttp or Jackson aren't suitable (e.g., GraalVM native image)

**Don't use this guide if:**
- You can use the standard `sdk-common-jvm` artifact (it includes ready-to-use implementations)

---

## Table of Contents

1. [Overview](#overview)
2. [Artifact Configuration](#artifact-configuration)
3. [Implementing ConfigurationParser](#implementing-configurationparser)
4. [Implementing EppoConfigurationClient](#implementing-eppoconfigurationclient)
5. [Complete Example: Gson Implementation](#complete-example-gson-implementation)
6. [Testing Your Implementation](#testing-your-implementation)
7. [Reference: sdk-common-jvm Source](#reference-sdk-common-jvm-source)

---

## Overview

The framework provides two interfaces you must implement:

| Interface | Purpose | Input | Output |
|-----------|---------|-------|--------|
| `ConfigurationParser<JSONFlagType>` | Parse JSON bytes to DTOs | Raw JSON bytes | Parsed DTO objects |
| `EppoConfigurationClient` | Fetch configuration from API | Request object | Response with bytes |

### Architecture

```
┌──────────────────────┐
│  Your Application    │
└──────────┬───────────┘
           │
    ┌──────▼────────┐
    │ BaseEppoClient│
    └──────┬────────┘
           │
           ├─────────────────┐
           │                 │
    ┌──────▼──────────┐ ┌───▼─────────────────┐
    │  Your Parser    │ │  Your HTTP Client    │
    │  Implementation │ │  Implementation      │
    └─────────────────┘ └──────────────────────┘
```

---

## Artifact Configuration

### Gradle

```groovy
dependencies {
    implementation 'cloud.eppo:eppo-sdk-framework:0.1.0'

    // Add your choice of JSON library
    implementation 'com.google.code.gson:gson:2.10.1'  // Example

    // Add your choice of HTTP library
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'  // Example
}

repositories {
    mavenCentral()
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>cloud.eppo</groupId>
        <artifactId>eppo-sdk-framework</artifactId>
        <version>0.1.0</version>
    </dependency>

    <!-- Add your JSON library -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>

    <!-- Add your HTTP library -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>
</dependencies>

```

---

## Implementing ConfigurationParser

### Interface Definition

```java
package cloud.eppo.parser;

import cloud.eppo.api.dto.BanditParametersResponse;
import cloud.eppo.api.dto.FlagConfigResponse;
import org.jetbrains.annotations.NotNull;

public interface ConfigurationParser<JSONFlagType> {

  /**
   * Parse raw flag configuration JSON bytes.
   */
  @NotNull FlagConfigResponse parseFlagConfig(@NotNull byte[] flagConfigJson)
      throws ConfigurationParseException;

  /**
   * Parse raw bandit parameters JSON bytes.
   */
  @NotNull BanditParametersResponse parseBanditParams(@NotNull byte[] banditParamsJson)
      throws ConfigurationParseException;

  /**
   * Parse a JSON value string to your JSON library's type.
   */
  @NotNull JSONFlagType parseJsonValue(@NotNull String jsonValue)
      throws ConfigurationParseException;
}
```

### Implementation Template

```java
package com.example;

import cloud.eppo.api.dto.*;
import cloud.eppo.parser.ConfigurationParser;
import cloud.eppo.parser.ConfigurationParseException;
import org.jetbrains.annotations.NotNull;

public class MyConfigurationParser implements ConfigurationParser<YourJsonType> {

    @Override
    @NotNull public FlagConfigResponse parseFlagConfig(@NotNull byte[] flagConfigJson)
            throws ConfigurationParseException {
        try {
            // 1. Parse JSON bytes to your library's object model
            YourJsonObject root = yourLibrary.parse(flagConfigJson);

            // 2. Extract flags map
            Map<String, FlagConfig> flags = parseFlags(root.get("flags"));

            // 3. Extract bandit references map
            Map<String, BanditReference> banditReferences =
                parseBanditReferences(root.get("bandits"));

            // 4. Extract format
            FlagConfigResponse.Format format = parseFormat(root.get("format"));

            // 5. Extract metadata
            String environmentName = root.getString("environment", null);
            Date createdAt = parseDate(root.get("createdAt"));

            // 6. Return FlagConfigResponse implementation
            return new FlagConfigResponse.Default(
                flags,
                banditReferences,
                format,
                environmentName,
                createdAt
            );

        } catch (Exception e) {
            throw new ConfigurationParseException(
                "Failed to parse flag configuration", e);
        }
    }

    @Override
    @NotNull public BanditParametersResponse parseBanditParams(@NotNull byte[] banditParamsJson)
            throws ConfigurationParseException {
        try {
            // 1. Parse JSON bytes
            YourJsonObject root = yourLibrary.parse(banditParamsJson);

            // 2. Extract bandits map
            Map<String, BanditParameters> bandits = parseBandits(root.get("bandits"));

            // 3. Return BanditParametersResponse implementation
            return new BanditParametersResponse.Default(bandits);

        } catch (Exception e) {
            throw new ConfigurationParseException(
                "Failed to parse bandit parameters", e);
        }
    }

    @Override
    @NotNull public YourJsonType parseJsonValue(@NotNull String jsonValue)
            throws ConfigurationParseException {
        try {
            return yourLibrary.parse(jsonValue);
        } catch (Exception e) {
            throw new ConfigurationParseException(
                "Failed to parse JSON value", e);
        }
    }

    // Helper methods to parse nested objects...
    private Map<String, FlagConfig> parseFlags(YourJsonObject flagsObj) {
        // Parse each flag to FlagConfig.Default
    }

    private Map<String, BanditReference> parseBanditReferences(YourJsonObject banditsObj) {
        // Parse each bandit reference to BanditReference.Default
    }
}
```

### Key Points

1. **Use Default Implementations:** All DTOs have nested `Default` classes you should instantiate
2. **Handle Nulls:** Many DTO fields are nullable - check the DTO interface definitions
3. **Date Parsing:** Dates are in ISO 8601 format (e.g., "2024-01-15T10:30:00Z")
4. **Format Enum:** `FlagConfigResponse.Format` has two values: `SERVER` and `CLIENT`
5. **Error Handling:** Wrap all exceptions in `ConfigurationParseException`

### DTO Interfaces Reference

All DTOs follow this pattern:

```java
public interface FlagConfig extends Serializable {
    @NotNull String getKey();
    boolean isEnabled();
    int getTotalShards();
    @NotNull VariationType getVariationType();
    @NotNull Map<String, Variation> getVariations();
    @NotNull List<Allocation> getAllocations();

    class Default implements FlagConfig {
        // Constructor and implementation
    }
}
```

See `src/main/java/cloud/eppo/api/dto/` for all DTO definitions.

---

## Implementing EppoConfigurationClient

### Interface Definition

```java
package cloud.eppo.http;

import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public interface EppoConfigurationClient {
  /**
   * Executes a configuration request asynchronously.
   *
   * The request may be either GET or POST based on request.getMethod().
   * For synchronous behavior, use .get() or .join() on the returned CompletableFuture.
   */
  @NotNull CompletableFuture<EppoConfigurationResponse> execute(
      @NotNull EppoConfigurationRequest request);
}
```

### Request Object

```java
public class EppoConfigurationRequest {
    @NotNull public String getBaseUrl();         // e.g., "https://fscdn.eppo.cloud"
    @NotNull public String getResourcePath();    // e.g., "/api/flag-config/v1/config"
    @NotNull public Map<String, String> getQueryParams();  // apiKey, sdkName, sdkVersion
    @Nullable public String getLastVersionId();  // For conditional requests (If-None-Match)
    @NotNull public HttpMethod getMethod();      // GET or POST
    @Nullable public byte[] getBody();           // Request body for POST requests
    @Nullable public String getContentType();    // Content type for POST requests
}
```

### Response Object

```java
public class EppoConfigurationResponse {
    public int getStatusCode();           // HTTP status code
    @Nullable public String getVersionId();  // ETag value from response
    @Nullable public byte[] getBody();       // Response body (null for 304)

    public boolean isSuccessful();        // true if 2xx
    public boolean isNotModified();       // true if 304

    // Factory methods
    public static EppoConfigurationResponse success(int statusCode, String versionId, byte[] body);
    public static EppoConfigurationResponse notModified(String versionId);
    public static EppoConfigurationResponse error(int statusCode, byte[] body);
}
```

### Implementation Template

```java
package com.example;

import cloud.eppo.http.*;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public class MyHttpClient implements EppoConfigurationClient {

    @Override
    @NotNull public CompletableFuture<EppoConfigurationResponse> execute(
            @NotNull EppoConfigurationRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Build full URL from request
                String url = buildUrl(request);

                // 2. Create HTTP request
                YourHttpRequest httpRequest = yourLibrary.newRequest()
                    .url(url)
                    .get();

                // 3. Add If-None-Match header if version ID present
                if (request.getLastVersionId() != null) {
                    httpRequest.addHeader("If-None-Match", request.getLastVersionId());
                }

                // 4. Execute request
                YourHttpResponse httpResponse = httpRequest.execute();

                // 5. Extract ETag from response headers
                String versionId = httpResponse.getHeader("ETag");

                // 6. Handle different status codes
                int statusCode = httpResponse.getStatusCode();

                if (statusCode == 304) {
                    // Not Modified
                    return EppoConfigurationResponse.notModified(versionId);

                } else if (statusCode >= 200 && statusCode < 300) {
                    // Success
                    byte[] body = httpResponse.getBodyBytes();
                    return EppoConfigurationResponse.success(statusCode, versionId, body);

                } else {
                    // Error
                    byte[] body = httpResponse.getBodyBytes();
                    return EppoConfigurationResponse.error(statusCode, body);
                }

            } catch (Exception e) {
                // Return error response on exception
                return EppoConfigurationResponse.error(500, null);
            }
        });
    }

    private String buildUrl(EppoConfigurationRequest request) {
        StringBuilder url = new StringBuilder();
        url.append(request.getBaseUrl());
        url.append(request.getResourcePath());

        // Add query parameters
        boolean first = true;
        for (Map.Entry<String, String> param : request.getQueryParams().entrySet()) {
            url.append(first ? "?" : "&");
            url.append(urlEncode(param.getKey()));
            url.append("=");
            url.append(urlEncode(param.getValue()));
            first = false;
        }

        return url.toString();
    }

    private String urlEncode(String value) {
        // Use URLEncoder.encode or your HTTP library's method
    }
}
```

### Key Points

1. **Asynchronous:** Return `CompletableFuture` - SDK will handle blocking if needed
2. **ETag Support:** Extract `ETag` header and return as `versionId`
3. **If-None-Match:** Send `lastVersionId` as `If-None-Match` header for conditional requests
4. **304 Handling:** Return `notModified()` response with null body
5. **Error Handling:** Catch exceptions and return error response
6. **Timeouts:** Configure appropriate connection and read timeouts
7. **POST Support:** Check `request.getMethod()` — for POST requests, include `request.getBody()` and `request.getContentType()`

---

## Complete Example: Gson Implementation

Here's a complete working example using Gson for JSON and OkHttp for HTTP.

### 1. Parser Implementation

```java
package com.example.eppo;

import cloud.eppo.api.dto.*;
import cloud.eppo.parser.ConfigurationParser;
import cloud.eppo.parser.ConfigurationParseException;
import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GsonConfigurationParser implements ConfigurationParser<JsonElement> {

    private final Gson gson = new GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .create();

    @Override
    @NotNull public FlagConfigResponse parseFlagConfig(@NotNull byte[] flagConfigJson)
            throws ConfigurationParseException {
        try {
            String jsonStr = new String(flagConfigJson, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();

            // Parse flags
            Map<String, FlagConfig> flags = new HashMap<>();
            if (root.has("flags")) {
                JsonObject flagsObj = root.getAsJsonObject("flags");
                for (Map.Entry<String, JsonElement> entry : flagsObj.entrySet()) {
                    flags.put(entry.getKey(), parseFlagConfig(entry.getValue().getAsJsonObject()));
                }
            }

            // Parse bandit references
            Map<String, BanditReference> banditRefs = new HashMap<>();
            if (root.has("bandits")) {
                JsonObject banditsObj = root.getAsJsonObject("bandits");
                for (Map.Entry<String, JsonElement> entry : banditsObj.entrySet()) {
                    banditRefs.put(entry.getKey(), parseBanditReference(entry.getValue().getAsJsonObject()));
                }
            }

            // Parse format
            FlagConfigResponse.Format format = FlagConfigResponse.Format.SERVER;
            if (root.has("format")) {
                format = FlagConfigResponse.Format.valueOf(root.get("format").getAsString());
            }

            // Parse metadata
            String environmentName = root.has("environment") ? root.get("environment").getAsString() : null;
            Date createdAt = root.has("createdAt") ? parseDate(root.get("createdAt").getAsString()) : null;

            return new FlagConfigResponse.Default(flags, banditRefs, format, environmentName, createdAt);

        } catch (Exception e) {
            throw new ConfigurationParseException("Failed to parse flag configuration", e);
        }
    }

    @Override
    @NotNull public BanditParametersResponse parseBanditParams(@NotNull byte[] banditParamsJson)
            throws ConfigurationParseException {
        try {
            String jsonStr = new String(banditParamsJson, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();

            Map<String, BanditParameters> bandits = new HashMap<>();
            if (root.has("bandits")) {
                JsonObject banditsObj = root.getAsJsonObject("bandits");
                for (Map.Entry<String, JsonElement> entry : banditsObj.entrySet()) {
                    bandits.put(entry.getKey(), parseBanditParameters(entry.getValue().getAsJsonObject()));
                }
            }

            return new BanditParametersResponse.Default(bandits);

        } catch (Exception e) {
            throw new ConfigurationParseException("Failed to parse bandit parameters", e);
        }
    }

    @Override
    @NotNull public JsonElement parseJsonValue(@NotNull String jsonValue)
            throws ConfigurationParseException {
        try {
            return JsonParser.parseString(jsonValue);
        } catch (Exception e) {
            throw new ConfigurationParseException("Failed to parse JSON value", e);
        }
    }

    // Helper methods...
    private FlagConfig parseFlagConfig(JsonObject obj) {
        String key = obj.get("key").getAsString();
        boolean enabled = obj.get("enabled").getAsBoolean();
        int totalShards = obj.get("totalShards").getAsInt();
        VariationType variationType = VariationType.valueOf(obj.get("variationType").getAsString());

        Map<String, Variation> variations = new HashMap<>();
        // Parse variations...

        List<Allocation> allocations = new ArrayList<>();
        // Parse allocations...

        return new FlagConfig.Default(key, enabled, totalShards, variationType, variations, allocations);
    }

    private Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.parse(dateStr);
    }

    // Additional helper methods for parsing other DTOs...
}
```

### 2. HTTP Client Implementation

```java
package com.example.eppo;

import cloud.eppo.http.*;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CustomOkHttpClient implements EppoConfigurationClient {

    private final OkHttpClient client;

    public CustomOkHttpClient() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    }

    @Override
    @NotNull public CompletableFuture<EppoConfigurationResponse> execute(
            @NotNull EppoConfigurationRequest request) {

        CompletableFuture<EppoConfigurationResponse> future = new CompletableFuture<>();

        // Build URL
        HttpUrl.Builder urlBuilder = HttpUrl.parse(
            request.getBaseUrl() + request.getResourcePath()
        ).newBuilder();

        for (Map.Entry<String, String> param : request.getQueryParams().entrySet()) {
            urlBuilder.addQueryParameter(param.getKey(), param.getValue());
        }

        // Build request
        Request.Builder reqBuilder = new Request.Builder()
            .url(urlBuilder.build())
            .get();

        // Add If-None-Match header if present
        if (request.getLastVersionId() != null) {
            reqBuilder.addHeader("If-None-Match", request.getLastVersionId());
        }

        // Execute asynchronously
        client.newCall(reqBuilder.build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    String etag = response.header("ETag");
                    int statusCode = response.code();

                    if (statusCode == 304) {
                        future.complete(EppoConfigurationResponse.notModified(etag));

                    } else if (response.isSuccessful() && response.body() != null) {
                        byte[] body = response.body().bytes();
                        future.complete(EppoConfigurationResponse.success(statusCode, etag, body));

                    } else {
                        byte[] body = response.body() != null ? response.body().bytes() : null;
                        future.complete(EppoConfigurationResponse.error(statusCode, body));
                    }

                } catch (IOException e) {
                    future.complete(EppoConfigurationResponse.error(500, null));
                } finally {
                    response.close();
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.complete(EppoConfigurationResponse.error(500, null));
            }
        });

        return future;
    }
}
```

### 3. Usage

```java
package com.example;

import cloud.eppo.BaseEppoClient;
import com.example.eppo.GsonConfigurationParser;
import com.example.eppo.CustomOkHttpClient;
import com.google.gson.JsonElement;

public class MyEppoClient extends BaseEppoClient<JsonElement> {

    public MyEppoClient(String apiKey) {
        super(
            apiKey,
            "my-sdk",
            "1.0.0",
            null,   // apiBaseUrl
            null,   // assignmentLogger
            null,   // banditLogger
            null,   // configurationStore
            false,  // isGracefulMode
            false,  // expectObfuscatedConfig
            true,   // supportBandits
            null,   // initialConfiguration
            null,   // assignmentCache
            null,   // banditAssignmentCache
            new GsonConfigurationParser(),  // Your parser
            new CustomOkHttpClient()        // Your HTTP client
        );
    }

    // Your client methods...
}
```

---

## Testing Your Implementation

### Unit Tests

```java
import org.junit.Test;
import static org.junit.Assert.*;

public class GsonConfigurationParserTest {

    @Test
    public void testParseFlagConfig() throws Exception {
        GsonConfigurationParser parser = new GsonConfigurationParser();

        String json = "{ \"flags\": {}, \"format\": \"SERVER\" }";
        FlagConfigResponse response = parser.parseFlagConfig(json.getBytes());

        assertNotNull(response);
        assertEquals(FlagConfigResponse.Format.SERVER, response.getFormat());
        assertTrue(response.getFlags().isEmpty());
    }

    @Test
    public void testParseJsonValue() throws Exception {
        GsonConfigurationParser parser = new GsonConfigurationParser();

        JsonElement element = parser.parseJsonValue("{\"key\": \"value\"}");

        assertTrue(element.isJsonObject());
        assertEquals("value", element.getAsJsonObject().get("key").getAsString());
    }
}
```

### Integration Tests

```java
import org.junit.Test;
import com.squareup.okhttp3.mockwebserver.MockWebServer;
import com.squareup.okhttp3.mockwebserver.MockResponse;

public class CustomOkHttpClientTest {

    @Test
    public void testSuccessfulRequest() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("ETag", "version123")
            .setBody("{\"flags\": {}}"));

        CustomOkHttpClient client = new CustomOkHttpClient();
        EppoConfigurationRequestFactory factory = new EppoConfigurationRequestFactory(
            server.url("/").toString(), "apiKey", "test", "1.0");

        EppoConfigurationRequest request = factory.createFlagConfigRequest(null);
        EppoConfigurationResponse response = client.execute(request).get();

        assertTrue(response.isSuccessful());
        assertEquals("version123", response.getVersionId());
        assertNotNull(response.getBody());

        server.shutdown();
    }

    @Test
    public void testNotModifiedResponse() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse()
            .setResponseCode(304)
            .setHeader("ETag", "version123"));

        CustomOkHttpClient client = new CustomOkHttpClient();
        EppoConfigurationRequestFactory factory = new EppoConfigurationRequestFactory(
            server.url("/").toString(), "apiKey", "test", "1.0");

        EppoConfigurationRequest request = factory.createFlagConfigRequest("version123");
        EppoConfigurationResponse response = client.execute(request).get();

        assertTrue(response.isNotModified());
        assertEquals("version123", response.getVersionId());
        assertNull(response.getBody());

        server.shutdown();
    }
}
```

---

## Reference: sdk-common-jvm Source

The `eppo-sdk-common` module contains reference implementations:

### JacksonConfigurationParser

Location: `eppo-sdk-common/src/main/java/cloud/eppo/JacksonConfigurationParser.java`

Shows how to:
- Use Jackson to parse configuration JSON
- Handle all DTO types
- Deal with null/optional fields
- Parse dates in ISO 8601 format

### OkHttpEppoClient

Location: `eppo-sdk-common/src/main/java/cloud/eppo/OkHttpEppoClient.java`

Shows how to:
- Use OkHttp for async requests
- Handle ETags and conditional requests
- Build URLs from request objects
- Handle different response codes

### JSON Deserializers

Location: `eppo-sdk-common/src/main/java/cloud/eppo/ufc/dto/adapters/`

Contains Jackson custom deserializers for:
- `FlagConfigResponse`
- `BanditParametersResponse`
- Date handling
- EppoValue serialization

---

## Common Pitfalls

### 1. Not Handling Nulls

Many DTO fields are nullable. Always check before accessing:

```java
// ❌ BAD
String env = flagConfigResponse.getEnvironmentName().toLowerCase();

// ✅ GOOD
String env = flagConfigResponse.getEnvironmentName();
if (env != null) {
    env = env.toLowerCase();
}
```

### 2. Incorrect Date Format

Dates must be parsed as ISO 8601 with UTC timezone:

```java
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
Date date = sdf.parse(dateString);
```

### 3. Not Using Default Implementations

Always use the nested `Default` classes when creating DTOs:

```java
// ❌ BAD - interface not instantiable
FlagConfig config = new FlagConfig(...);

// ✅ GOOD - use nested Default class
FlagConfig config = new FlagConfig.Default(...);
```

### 4. Blocking on CompletableFuture

Don't block the calling thread in the HTTP client implementation:

```java
// ❌ BAD - blocks in async method
public CompletableFuture<EppoConfigurationResponse> execute(EppoConfigurationRequest request) {
    Response response = httpClient.send(request);  // Blocking!
    return CompletableFuture.completedFuture(parseResponse(response));
}

// ✅ GOOD - truly async
public CompletableFuture<EppoConfigurationResponse> execute(EppoConfigurationRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        Response response = httpClient.execute(request);
        return parseResponse(response);
    });
}
```

### 5. Not Handling 304 Correctly

304 Not Modified responses have no body:

```java
if (statusCode == 304) {
    return EppoConfigurationResponse.notModified(versionId);  // body is null
} else if (statusCode == 200) {
    return EppoConfigurationResponse.success(statusCode, versionId, body);
}
```

---

## Support

- **Framework Source:** https://github.com/Eppo-exp/sdk-common-jdk
- **Reference Implementation:** See `eppo-sdk-common/` directory
- **Issues:** https://github.com/Eppo-exp/sdk-common-jdk/issues
- **Migration Guide:** See `MIGRATION_GUIDE_v4.md` for upgrading from v3.x

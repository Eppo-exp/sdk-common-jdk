<!-- GSD:project-start source:PROJECT.md -->
## Project

**setConfiguration Public API**

Adds a `setConfiguration(ConfigurationType)` method to `BaseEppoClient` so callers can push a configuration object directly without triggering a remote fetch. The method is synchronous, fires all registered `onConfigurationChange` subscribers, and follows last-write-wins semantics. It stacks on PR 243 (`typo/generic-configuration`), which made `Configuration` generic.

**Core Value:** SDK consumers can supply a configuration object at runtime — from cache, a test fixture, or another source — and the client immediately reflects it with full notification propagation.

### Constraints

- **Java version**: Java 8 source/target — no modern Java features
- **Thread safety**: `ConfigurationStore` uses `volatile`; `CallbackManager` uses `ConcurrentHashMap`; notification must remain synchronized as existing code does
- **Base branch**: Must stack on `typo/generic-configuration` (PR 243), not `main`
- **No HTTP/JSON libraries in root project**: `setConfiguration` lives in `BaseEppoClient` (root) — implementation must not import OkHttp or Jackson
<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->
## Technology Stack

## Languages
- Java 8+ (source/target compatibility `1_8`) - All production and test code
## Runtime
- JVM (Java 8, 11, 17, 21 tested in CI)
- CI uses AdoptOpenJDK and Eclipse Temurin distributions
- Gradle 8.6 (via Gradle Wrapper)
- Lockfile: Not used (no dependency locking configured)
## Project Structure
| Module | ArtifactId | Purpose |
|--------|-----------|---------|
| Root (`:`) | `eppo-sdk-framework` | Core framework with interfaces and evaluation logic |
| `eppo-sdk-common` | `sdk-common-jvm` | Default implementations (OkHttp + Jackson) |
- `settings.gradle` defines root project name `sdk-common-jvm` and includes `eppo-sdk-common`
- `eppo-sdk-common` depends on root via `api project(':')`
- Dependency resolution uses Maven Central and JitPack (`https://jitpack.io`)
## Frameworks
- No application framework. This is a library SDK.
- JUnit 5 (Jupiter) `5.14.3` - Test runner and assertions (`build.gradle` line 48)
- Google Truth `1.4.5` - Fluent assertions (`build.gradle` line 54)
- Mockito `4.11.0` - Mocking (with ByteBuddy `1.18.5` override for Java 21 compatibility) (`build.gradle` lines 55-59)
- JSONAssert `1.5.3` - JSON comparison in tests (`build.gradle` line 52)
- OkHttp MockWebServer `4.12.0` - HTTP mocking (`build.gradle` line 49)
- Gradle 8.6 - Build tool (`gradle/wrapper/gradle-wrapper.properties`)
- Spotless (8.1.0 on Java 17+, 6.13.0 otherwise) - Code formatting with Google Java Format (`build.gradle` lines 1-15, 78-102)
- JReleaser 1.18.0 - Release/publish automation. Pinned to 1.18; 1.20.0 has publish failures. (`build.gradle` line 20)
## Key Dependencies
- `org.jetbrains:annotations:26.1.0` - Nullability annotations (`@NotNull`, `@Nullable`). Exposed as `api` dependency. (`build.gradle` line 42)
- `com.github.zafarkhaja:java-semver:0.10.2` - Semantic version parsing (`build.gradle` line 43)
- `org.apache.commons:commons-collections4:4.5.0` - LRU and expiring map implementations for caching (`build.gradle` line 45)
- `org.slf4j:slf4j-api:2.0.17` - Logging facade (`build.gradle` line 46)
- `com.squareup.okhttp3:okhttp:4.12.0` - HTTP client for Eppo API communication (`eppo-sdk-common/build.gradle` line 23)
- `com.fasterxml.jackson.core:jackson-databind:2.20.1` - JSON deserialization of flag configs and bandit parameters (`eppo-sdk-common/build.gradle` line 24)
- `com.fasterxml.jackson.core:jackson-databind:2.20.1` - Also used in root module tests (`build.gradle` line 60)
- `com.squareup.okhttp3:okhttp:4.12.0` - Also used in root module tests (`build.gradle` line 61)
- `commons-io:commons-io:2.21.0` - File I/O utilities for test data loading (`build.gradle` line 53)
- `org.slf4j:slf4j-simple:2.0.17` - SLF4J binding for test logging output (`build.gradle` line 47)
## Configuration
- SDK Key (API key) provided at client initialization, not via environment variable
- Base URL defaults to `https://fscdn.eppo.cloud/api` (`src/main/java/cloud/eppo/Constants.java`)
- SDK Key can encode a customer-specific subdomain for routing (`src/main/java/cloud/eppo/SDKKey.java`)
- `build.gradle` - Root module build config
- `eppo-sdk-common/build.gradle` - Common module build config
- `settings.gradle` - Multi-module project settings
- `Makefile` - Convenience targets for `test-data`, `build`, `test`
- `gradle/wrapper/gradle-wrapper.properties` - Gradle wrapper version
- Google Java Format enforced via Spotless
- Ratchet mode: only checks changed files relative to `origin/main`
- Version selection adapts to Java runtime (1.17.0 for Java 17+, 1.7 for older)
## Publishing
- `cloud.eppo:eppo-sdk-framework` (root module) - version `0.1.0-SNAPSHOT`
- `cloud.eppo:sdk-common-jvm` (eppo-sdk-common module) - version `4.0.0-SNAPSHOT`
- Root module also publishes a test JAR (`testJar` task in `build.gradle` lines 104-107)
- Artifacts staged to `build/staging-deploy`
- JReleaser deploys to Maven Central via Sonatype (`https://central.sonatype.com`)
- Snapshots go to Sonatype snapshot repository
- GPG signing required (`signing.active = 'ALWAYS'`)
- Version check task prevents accidental release/snapshot mismatches (`build.gradle` lines 190-208)
## Platform Requirements
- JDK 8+ (JDK 17+ recommended for full Spotless/formatting support)
- Git (for test data checkout and Spotless ratchet mode)
- Java 8+ runtime
- Network access to `fscdn.eppo.cloud` (or custom base URL)
- Fetched from `https://github.com/Eppo-exp/sdk-test-data.git` via `make test-data`
- Placed in `src/test/resources/shared/ufc/`
- Dynamic typing bandit tests excluded (`rm -f *dynamic-typing.json`)
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

## Naming Patterns
- PascalCase for all Java class files: `BaseEppoClient.java`, `FlagEvaluator.java`, `ConfigurationRequestor.java`
- Interface files use `I` prefix for store/cache contracts: `IConfigurationStore.java`, `IAssignmentCache.java`
- DTO interfaces do not use `I` prefix: `FlagConfig.java`, `Variation.java`
- Exception classes use `Exception` suffix: `InvalidApiKeyException.java`, `ConfigurationParseException.java`
- Root package: `cloud.eppo`
- Sub-packages by concern: `cloud.eppo.api`, `cloud.eppo.api.dto`, `cloud.eppo.cache`, `cloud.eppo.callback`, `cloud.eppo.http`, `cloud.eppo.logging`, `cloud.eppo.model`, `cloud.eppo.parser`, `cloud.eppo.exception`
- Adapter/serializer classes in `cloud.eppo.ufc.dto.adapters` (eppo-sdk-common module)
- camelCase for all methods: `evaluateFlag()`, `getBooleanAssignment()`, `fetchAndSaveFromRemote()`
- Getter prefix `get` for accessors: `getConfiguration()`, `getFlagKey()`, `getVariation()`
- Boolean getters use `is` prefix: `isEnabled()`, `isConfigObfuscated()`, `isEmpty()`
- Factory methods use `create` prefix: `createFlagConfigRequest()`, `createBanditParamsRequest()`
- Static factory methods use `valueOf()` on value types: `EppoValue.valueOf(true)`, `EppoValue.nullValue()`
- Builder methods return `this` for chaining: `builder.flagEvaluationCode(...).variationKey(...).build()`
- camelCase for all local and instance variables: `subjectKey`, `flagKey`, `configFetchedAt`
- Constants use UPPER_SNAKE_CASE: `DEFAULT_POLLING_INTERVAL_MILLIS`, `DEFAULT_BASE_URL`
- Logger instances named `log`: `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`
- Boolean fields prefixed with `is` or descriptive: `isGracefulMode`, `isConfigObfuscated`, `doLog`
- PascalCase for all types: `FlagEvaluationResult`, `BanditEvaluationResult`, `AssignmentDetails`
- Enums use PascalCase names with UPPER_SNAKE_CASE values: `FlagEvaluationCode.FLAG_UNRECOGNIZED_OR_DISABLED`
- Generic type parameters use descriptive names: `<JsonFlagType>`, `<JSONFlagType>`, `<T>`
## Code Style
- Spotless with Google Java Format enforced via Gradle plugin
- Config: `build.gradle` lines 78-102
- Ratchet from `origin/main` -- only changed files are checked
- Version-conditional formatting: google-java-format 1.17.0 for Java 17+, 1.7 for Java 8-16
- 2-space indentation for Gradle files
- Trailing whitespace trimmed, files end with newline
- No separate linter (ESLint/Checkstyle). Spotless handles formatting only.
- JetBrains `@NotNull` and `@Nullable` annotations used for nullability: `org.jetbrains:annotations:26.1.0`
- `@SuppressWarnings("AnonymousHasLambdaAlternative")` used in `Utils.java` for Android compatibility (avoids lambdas in ThreadLocal initializers)
- Source and target compatibility: Java 8
- Avoid Java 9+ APIs for Android compatibility (e.g., use `SimpleDateFormat` instead of `DateTimeFormatter`, manual string joining instead of `String.join`)
## Import Organization
- None. Standard Java package imports.
- Used occasionally in test files: `import static org.junit.jupiter.api.Assertions.*;`
- Used occasionally for same-package DTOs: `import cloud.eppo.api.*;`
## Error Handling
- `InvalidApiKeyException` extends `RuntimeException` (`src/main/java/cloud/eppo/exception/InvalidApiKeyException.java`)
- `ConfigurationParseException` for parse failures (`src/main/java/cloud/eppo/parser/ConfigurationParseException.java`)
- Return `null` when items not found (e.g., `getFlag()` returns `null` for unknown keys)
- Use `@Nullable` / `@NotNull` annotations from JetBrains
- Defensive null checks on collections: default to `Collections.emptyMap()` / `Collections.emptyList()`
## Logging
- `log.debug()` - Operational flow: "Fetching configuration", "Started polling at...", "Loaded N flag definitions"
- `log.info()` - Graceful fallback messages: "error getting assignment value: {}", "no assigned variation because..."
- `log.warn()` - Missing config or non-critical failures: "no configuration found for key: {}", "Error logging bandit assignment"
- `log.error()` - Exceptions during config fetch/parse: "Encountered Exception while loading configuration", "Failed to parse flag configuration"
## Comments
- Explain "why" not "what" for non-obvious design decisions
- Document Android compatibility constraints: `// Android API version 21 does not have access to...`
- Mark workarounds and known issues with TODO/FIXME
- Short inline comments for operational notes: `// We don't want to fetch right away`
- Applied to public API interfaces and their methods (`EppoConfigurationClient`, `ConfigurationParser`, `IConfigurationStore`)
- Applied to public methods on `BaseEppoClient` (the primary consumer API)
- Builder/Configuration classes have usage examples in class-level Javadoc with `{@code}` blocks
- Internal/package-private methods have minimal or no Javadoc
## Function Design
- Typed returns for assignment methods: `boolean`, `int`, `Double`, `String`, `JsonFlagType`
- `AssignmentDetails<T>` for detailed variants
- `null` for "not found" scenarios
- `CompletableFuture<Void>` for async operations
## Module Design
- Public classes serve as the API surface
- Package-private visibility for internal methods (e.g., `Configuration` constructor, `Utils.resetBase64Codec()`)
- `protected` on `BaseEppoClient` methods intended for SDK extension: `loadConfiguration()`, `startPolling()`, `evaluateAndMaybeLog()`
- DTOs use interface with nested `Default` class: `FlagConfig` interface with `FlagConfig.Default` implementation (`src/main/java/cloud/eppo/api/dto/FlagConfig.java`)
- Same pattern for `Variation`, `TargetingCondition`, `TargetingRule`, `Shard`, `Split`, `Allocation`
- This allows the framework module to define contracts while implementations can provide their own deserialization
- Used for complex immutable objects: `Configuration.Builder`, `EvaluationDetails.Builder`, `FlagEvaluationResult.Builder`
- Copy-from constructors for creating modified copies: `EvaluationDetails.builder(existingDetails)`
- Static factory method: `EvaluationDetails.buildDefault(...)` for common error cases
- Collections wrapped in `Collections.unmodifiableMap()` / `Collections.unmodifiableList()` in constructors
- `Configuration` is immutable after construction
- DTOs use `final` fields
- `ConcurrentHashMap` for callback subscribers (`CallbackManager.java`)
- `synchronized` blocks around config store access during async operations
- `volatile` for `Base64Codec` static field in `Utils.java`
- `ThreadLocal` for `MessageDigest` and `SimpleDateFormat` instances
## Two-Module Architecture Convention
- Contains all core interfaces: `EppoConfigurationClient`, `ConfigurationParser`, `IConfigurationStore`
- Contains all evaluation logic: `FlagEvaluator`, `RuleEvaluator`, `BanditEvaluator`
- Contains `BaseEppoClient` parameterized by `<JsonFlagType>`
- No dependency on Jackson or OkHttp
- Published as `cloud.eppo:eppo-sdk-framework`
- `OkHttpEppoClient` implements `EppoConfigurationClient`
- `JacksonConfigurationParser` implements `ConfigurationParser<JsonNode>`
- Jackson deserializers in `cloud.eppo.ufc.dto.adapters`
- Published as `cloud.eppo:sdk-common-jvm`
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

## System Overview
```text
```
## Component Responsibilities
| Component | Responsibility | File |
|-----------|----------------|------|
| `BaseEppoClient` | Core SDK client with typed assignment methods, bandit actions, polling, graceful mode | `src/main/java/cloud/eppo/BaseEppoClient.java` |
| `ConfigurationRequestor` | Fetches config from remote API, manages initial config race conditions, notifies change listeners | `src/main/java/cloud/eppo/ConfigurationRequestor.java` |
| `ConfigurationStore` | Default in-memory volatile storage for `Configuration` | `src/main/java/cloud/eppo/ConfigurationStore.java` |
| `Configuration` | Immutable snapshot of flags + bandits + metadata, built via Builder pattern | `src/main/java/cloud/eppo/api/Configuration.java` |
| `FlagEvaluator` | Static evaluation of flags against subject attributes through allocations, rules, and shard matching | `src/main/java/cloud/eppo/FlagEvaluator.java` |
| `BanditEvaluator` | Contextual bandit evaluation: scores actions, computes weights, selects action deterministically | `src/main/java/cloud/eppo/BanditEvaluator.java` |
| `RuleEvaluator` | Evaluates targeting rules/conditions against subject attributes (includes obfuscation support) | `src/main/java/cloud/eppo/RuleEvaluator.java` |
| `FetchConfigurationTask` | `TimerTask` for jittered periodic config polling | `src/main/java/cloud/eppo/FetchConfigurationTask.java` |
| `EppoConfigurationClient` | Interface for HTTP transport (pluggable) | `src/main/java/cloud/eppo/http/EppoConfigurationClient.java` |
| `ConfigurationParser` | Interface for JSON deserialization (pluggable) | `src/main/java/cloud/eppo/parser/ConfigurationParser.java` |
| `IConfigurationStore` | Interface for configuration persistence (pluggable) | `src/main/java/cloud/eppo/IConfigurationStore.java` |
| `CommonEppoClient` | Convenience subclass wiring OkHttp + Jackson defaults | `eppo-sdk-common/src/main/java/cloud/eppo/CommonEppoClient.java` |
| `OkHttpEppoClient` | OkHttp implementation of `EppoConfigurationClient` with ETag/304 support | `eppo-sdk-common/src/main/java/cloud/eppo/OkHttpEppoClient.java` |
| `JacksonConfigurationParser` | Jackson implementation of `ConfigurationParser<JsonNode>` | `eppo-sdk-common/src/main/java/cloud/eppo/JacksonConfigurationParser.java` |
| `CallbackManager` | Generic pub/sub for configuration change notifications | `src/main/java/cloud/eppo/callback/CallbackManager.java` |
| `ApiEndpoints` | URL construction with SDK key subdomain resolution | `src/main/java/cloud/eppo/ApiEndpoints.java` |
| `EppoConfigurationRequestFactory` | Creates typed HTTP requests for flag config and bandit params | `src/main/java/cloud/eppo/http/EppoConfigurationRequestFactory.java` |
## Pattern Overview
- Root project (`eppo-sdk-framework`) contains all core logic and defines pluggable interfaces
- `eppo-sdk-common` (`sdk-common-jvm`) provides default implementations (OkHttp + Jackson) and depends on the root
- Platform SDKs (Java Server, Android) extend `BaseEppoClient` or `CommonEppoClient` for their runtime
- Configuration is immutable; all mutation goes through `Configuration.Builder`
- Evaluators (`FlagEvaluator`, `BanditEvaluator`, `RuleEvaluator`) are stateless static utility classes
## Layers
- Purpose: Typed assignment methods consumed by platform SDKs and end users
- Location: `src/main/java/cloud/eppo/BaseEppoClient.java`
- Contains: `get{Boolean,Integer,Double,String,JSON}Assignment()`, `getBanditAction()`, `*Details()` variants
- Depends on: Evaluators, ConfigurationStore, Loggers, Caches
- Used by: Platform SDK implementations (e.g., `CommonEppoClient`)
- Purpose: Stateless flag/bandit/rule evaluation logic
- Location: `src/main/java/cloud/eppo/FlagEvaluator.java`, `BanditEvaluator.java`, `RuleEvaluator.java`
- Contains: Pure evaluation functions operating on DTO inputs
- Depends on: DTOs (`cloud.eppo.api.dto.*`), `Utils` (hashing, sharding)
- Used by: `BaseEppoClient`
- Purpose: Fetching, parsing, storing, and notifying about configuration
- Location: `src/main/java/cloud/eppo/ConfigurationRequestor.java`, `ConfigurationStore.java`, `api/Configuration.java`
- Contains: Config fetch orchestration, immutable config snapshots, change callbacks
- Depends on: `EppoConfigurationClient` (interface), `ConfigurationParser` (interface), `IConfigurationStore` (interface)
- Used by: `BaseEppoClient`
- Purpose: Transport abstraction for configuration fetching
- Location: `src/main/java/cloud/eppo/http/`
- Contains: Request/response value objects, client interface, request factory
- Depends on: Nothing (pure interfaces and value types)
- Used by: `ConfigurationRequestor`
- Purpose: JSON deserialization abstraction
- Location: `src/main/java/cloud/eppo/parser/`
- Contains: `ConfigurationParser<T>` interface and parse exception
- Depends on: DTOs
- Used by: `ConfigurationRequestor`, `BaseEppoClient` (for JSON value unwrapping)
- Purpose: Data transfer objects for flag configuration and bandit parameters
- Location: `src/main/java/cloud/eppo/api/dto/`
- Contains: `FlagConfig`, `Allocation`, `Variation`, `Split`, `Shard`, `TargetingRule`, `BanditParameters`, etc.
- Depends on: `EppoValue`, `ShardRange`
- Used by: All layers
- Purpose: Assignment and bandit event logging abstraction
- Location: `src/main/java/cloud/eppo/logging/`
- Contains: `AssignmentLogger`, `BanditLogger` interfaces, `Assignment`, `BanditAssignment` data classes
- Depends on: Nothing
- Used by: `BaseEppoClient`
- Purpose: Assignment deduplication
- Location: `src/main/java/cloud/eppo/cache/`
- Contains: `IAssignmentCache` interface, LRU/expiring/non-expiring implementations
- Depends on: Apache Commons Collections4
- Used by: `BaseEppoClient`
## Data Flow
### Primary: Flag Assignment
### Secondary: Bandit Action Selection
### Configuration Fetch Flow
- `Configuration` is immutable; stored as `volatile` reference in `ConfigurationStore`
- `ConfigurationRequestor` manages race conditions between initial config (async) and remote fetch via `synchronized` blocks
- Polling uses `java.util.Timer` with jittered intervals via `FetchConfigurationTask`
- Assignment caches use `IAssignmentCache` implementations for log deduplication (LRU, expiring, non-expiring)
## Key Abstractions
- Purpose: Generic base class parameterized on JSON type (e.g., Jackson `JsonNode`, Gson `JsonElement`)
- Examples: `CommonEppoClient extends BaseEppoClient` (wires `JsonNode`)
- Pattern: Template Method -- subclasses provide initialization, base class provides all evaluation logic
- Purpose: Immutable snapshot of all flag configs, bandit parameters, and metadata
- Examples: `src/main/java/cloud/eppo/api/Configuration.java`
- Pattern: Builder pattern with conditional bandit loading (`requiresUpdatedBanditModels()`)
- Purpose: Pluggable HTTP transport for fetching config
- Examples: `OkHttpEppoClient` in `eppo-sdk-common`
- Pattern: Strategy pattern -- platform SDKs can provide their own HTTP client
- Purpose: Pluggable JSON deserialization
- Examples: `JacksonConfigurationParser` in `eppo-sdk-common`
- Pattern: Strategy pattern -- allows Jackson, Gson, or custom parsers
- Purpose: Pluggable configuration persistence (in-memory, disk, etc.)
- Examples: `ConfigurationStore` (volatile in-memory default)
- Pattern: Strategy pattern -- Android SDK uses persistent store
- Purpose: Union type wrapping boolean, double, string, string array, or null
- Examples: `src/main/java/cloud/eppo/api/EppoValue.java`
- Pattern: Tagged union with type checking methods (`isBoolean()`, `isNumeric()`, etc.)
## Entry Points
- Location: `src/main/java/cloud/eppo/BaseEppoClient.java:53`
- Triggers: Platform SDK initialization
- Responsibilities: Wires all dependencies, creates requestor, optionally sets initial config
- Location: `src/main/java/cloud/eppo/BaseEppoClient.java:109`
- Triggers: SDK init, polling timer
- Responsibilities: Synchronous config fetch + store
- Location: `src/main/java/cloud/eppo/BaseEppoClient.java:127`
- Triggers: Platform SDK after initial load
- Responsibilities: Sets up periodic config refresh with jitter
- Location: `src/main/java/cloud/eppo/BaseEppoClient.java:386-590`
- Triggers: Application code
- Responsibilities: Type-safe flag evaluation with logging
## Architectural Constraints
- **Java version:** Source and target compatibility is Java 8 (`JavaVersion.VERSION_1_8`). No `Optional`, no `var`, no records.
- **Threading:** Config polling uses `java.util.Timer` (daemon thread). `ConfigurationStore` uses `volatile` for thread-safe reads. `ConfigurationRequestor` uses `synchronized` blocks for fetch race conditions. `CallbackManager` uses `ConcurrentHashMap`.
- **Global state:** None at the framework level. Each `BaseEppoClient` instance is self-contained. Platform SDKs may maintain singletons.
- **Circular imports:** None detected. Dependency flow is strictly: client -> evaluators -> DTOs.
- **Obfuscation support:** The entire evaluation pipeline handles both obfuscated (MD5 hashed keys, base64 encoded values) and plaintext configurations, controlled by `isConfigObfuscated` flag in `Configuration`.
- **ProGuard safety:** Jackson deserializers in `eppo-sdk-common` are hand-rolled (no annotation reliance) per `JacksonConfigurationParser` javadoc.
- **No annotation processing:** DTOs avoid Jackson annotations to support ProGuard/R8 minification on Android.
## Anti-Patterns
### Putting HTTP or JSON library code in the root project
### Mutable Configuration objects
## Error Handling
- When `isGracefulMode=true` (default), evaluation errors return the caller's default value and log at INFO level
- When `isGracefulMode=false`, evaluation errors throw `RuntimeException`
- Config fetch errors are caught in `loadConfiguration()` and either swallowed (graceful) or rethrown
- `FetchConfigurationTask.run()` always catches and logs errors (polling never crashes)
- Logger errors (in `assignmentLogger.logAssignment()`) are always caught and logged, never propagated
## Cross-Cutting Concerns
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->

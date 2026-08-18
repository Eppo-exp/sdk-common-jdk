# setConfiguration Public API

## What This Is

Adds a `setConfiguration(ConfigurationType)` method to `BaseEppoClient` so callers can push a configuration object directly without triggering a remote fetch. The method is synchronous, fires all registered `onConfigurationChange` subscribers, and follows last-write-wins semantics. It stacks on PR 243 (`typo/generic-configuration`), which made `Configuration` generic.

## Core Value

SDK consumers can supply a configuration object at runtime — from cache, a test fixture, or another source — and the client immediately reflects it with full notification propagation.

## Requirements

### Validated

- ✓ `Configuration` is generic (`ConfigurationType extends SerializableEppoConfiguration`) — via PR 243
- ✓ `onConfigurationChange` / `unsubscribeFromConfigurationChange` subscription API exists — via `CallbackManager` in `ConfigurationRequestor`
- ✓ `saveConfigurationAndNotify()` in `ConfigurationRequestor` saves to store and fires all subscribers — existing private method

### Active

- [ ] `BaseEppoClient.setConfiguration(ConfigurationType config)` — public, void, synchronous
- [ ] Delegates to `ConfigurationRequestor` which calls `saveConfigurationAndNotify().join()`
- [ ] Fires all registered `onConfigurationChange` callbacks synchronously before returning
- [ ] `@NonNull` annotation on `config` parameter; null in graceful mode = silently ignored; null in non-graceful mode = `IllegalArgumentException`
- [ ] In-flight remote fetches are NOT cancelled — last write wins; a fetch completing after `setConfiguration` will overwrite it
- [ ] `CommonEppoClient` inherits the method from `BaseEppoClient` — no additional surface needed

### Out of Scope

- Cancelling in-flight remote fetches — conflicts with last-write-wins design; callers who need fetch suppression can stop polling
- Async overload (`setConfigurationAsync`) — not requested; sync is sufficient
- Persistence to `IConfigurationStore` beyond what `saveConfiguration()` already does — no new storage contracts
- Bandit re-fetch when a configuration is injected — injected config is treated as authoritative as-is

## Context

### Codebase structure

- Root project (`eppo-sdk-framework`): `BaseEppoClient`, `ConfigurationRequestor`, `ConfigurationStore`, `FlagEvaluator`
- Subproject (`eppo-sdk-common`): `CommonEppoClient` (extends `BaseEppoClient`), `JacksonConfigurationParser`, `OkHttpEppoClient`
- `ConfigurationRequestor` owns the `CallbackManager<ConfigurationType>` and all save+notify logic
- `saveConfigurationAndNotify(ConfigurationType)` is currently private — must be made package-private or a new public delegation method added

### Notification path

`saveConfigurationAndNotify` → `IConfigurationStore.saveConfiguration()` → `CallbackManager.notifyCallbacks()`. All three steps must happen for `setConfiguration` to fulfill its contract.

### PR 243 context

PR 243 (`typo/generic-configuration`) replaces 3-param `ConfigurationParser` with a 2-param design and removes phantom `ConfigurationBuilderType`. `BaseEppoClient` is now `BaseEppoClient<ConfigurationType extends SerializableEppoConfiguration, JsonFlagType>`. The `setConfiguration` signature must use `ConfigurationType` to be compatible.

### Java version constraint

Source/target compatibility is Java 8. No `Optional`, no `var`, no records.

## Constraints

- **Java version**: Java 8 source/target — no modern Java features
- **Thread safety**: `ConfigurationStore` uses `volatile`; `CallbackManager` uses `ConcurrentHashMap`; notification must remain synchronized as existing code does
- **Base branch**: Must stack on `typo/generic-configuration` (PR 243), not `main`
- **No HTTP/JSON libraries in root project**: `setConfiguration` lives in `BaseEppoClient` (root) — implementation must not import OkHttp or Jackson

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Last write wins — no fetch cancellation | Simpler contract; avoids race condition logic; callers can stop polling if they want full control | — Pending |
| Null in graceful mode = silent ignore | Consistent with existing graceful-mode philosophy (SDK never throws in graceful mode) | — Pending |
| Delegate through `ConfigurationRequestor` | Keeps notification logic in one place; avoids duplicating `saveConfigurationAndNotify` | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-08-18 after initialization*

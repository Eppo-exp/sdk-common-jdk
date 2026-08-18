# Requirements: setConfiguration Public API

**Defined:** 2026-08-18
**Core Value:** SDK consumers can push a configuration object at runtime and the client immediately reflects it with full notification propagation.

## v1 Requirements

### API Surface

- [ ] **API-01**: `BaseEppoClient.setConfiguration(ConfigurationType config)` is a public, void, synchronous method
- [ ] **API-02**: Method accepts `@NonNull ConfigurationType config` parameter
- [ ] **API-03**: When `config` is null and graceful mode is enabled, the method returns silently without error
- [ ] **API-04**: When `config` is null and graceful mode is disabled, the method throws `IllegalArgumentException`

### Configuration Storage

- [ ] **STORE-01**: Calling `setConfiguration` saves the provided configuration to `IConfigurationStore` via `saveConfiguration()`
- [ ] **STORE-02**: The store write is synchronous — the configuration is readable via `getConfiguration()` before `setConfiguration` returns

### Notifications

- [ ] **NOTIF-01**: All registered `onConfigurationChange` subscribers are notified synchronously before `setConfiguration` returns
- [ ] **NOTIF-02**: Subscribers receive the exact `ConfigurationType` instance that was passed to `setConfiguration`

### Concurrency

- [ ] **CONC-01**: In-flight remote fetches are not cancelled when `setConfiguration` is called (last write wins — a completing fetch will overwrite the manually set config)
- [ ] **CONC-02**: The method is thread-safe — concurrent calls from multiple threads do not corrupt state

### Delegation

- [ ] **DELEG-01**: The implementation in `BaseEppoClient` delegates to `ConfigurationRequestor` (no duplication of save/notify logic)
- [ ] **DELEG-02**: `CommonEppoClient` inherits the method without additional changes

## v2 Requirements

- **ASYNC-01**: Async overload `setConfigurationAsync(ConfigurationType)` returning `CompletableFuture<Void>` — deferred; sync sufficient for v1
- **CANCEL-01**: Option to cancel in-flight fetches on `setConfiguration` — deferred; callers who need this can stop polling

## Out of Scope

| Feature | Reason |
|---------|--------|
| Bandit re-fetch on injected config | Injected config is treated as authoritative as-is |
| Persistence beyond existing `saveConfiguration()` | No new storage contracts needed |
| Async variant | Not requested; sync is sufficient |
| Fetch cancellation | Conflicts with last-write-wins design |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| API-01 | Phase 1 | Pending |
| API-02 | Phase 1 | Pending |
| API-03 | Phase 1 | Pending |
| API-04 | Phase 1 | Pending |
| STORE-01 | Phase 1 | Pending |
| STORE-02 | Phase 1 | Pending |
| NOTIF-01 | Phase 1 | Pending |
| NOTIF-02 | Phase 1 | Pending |
| CONC-01 | Phase 1 | Pending |
| CONC-02 | Phase 1 | Pending |
| DELEG-01 | Phase 1 | Pending |
| DELEG-02 | Phase 1 | Pending |

**Coverage:**
- v1 requirements: 12 total
- Mapped to phases: 12
- Unmapped: 0 ✓

---
*Requirements defined: 2026-08-18*
*Last updated: 2026-08-18 after initial definition*

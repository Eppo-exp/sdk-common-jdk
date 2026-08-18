# Roadmap: setConfiguration Public API

**Generated:** 2026-08-18
**Total Phases:** 1
**Requirements Coverage:** 12/12 ✓

---

### Phase 1: Implement setConfiguration

**Goal:** Add `setConfiguration(ConfigurationType)` to `BaseEppoClient` and `ConfigurationRequestor`, with proper null handling, synchronous notification firing, and thread safety.

**Requirements:** API-01, API-02, API-03, API-04, STORE-01, STORE-02, NOTIF-01, NOTIF-02, CONC-01, CONC-02, DELEG-01, DELEG-02

**Success Criteria:**
1. `client.setConfiguration(config)` is callable and immediately readable via `client.getConfiguration()`
2. All subscribers registered via `onConfigurationChange()` are called synchronously before `setConfiguration` returns
3. Passing null with graceful mode on is a no-op; with graceful mode off it throws `IllegalArgumentException`
4. Concurrent calls from multiple threads leave the store in a consistent (non-corrupt) state
5. All existing tests pass; new unit tests cover the above behaviors

**Branch:** `typo/set-configuration` (stacked on `typo/generic-configuration`)

**Implementation notes:**
- `ConfigurationRequestor.saveConfigurationAndNotify()` is currently private — make it package-private OR add a new `public void setConfiguration(ConfigurationType)` method to `ConfigurationRequestor` that calls `saveConfigurationAndNotify(config).join()`
- `BaseEppoClient.setConfiguration(ConfigurationType)` is public, delegates to `requestor.setConfiguration(config)`
- Null guard lives in `BaseEppoClient` before the delegate call (checks `isGracefulMode`)
- No changes to `CommonEppoClient` needed (inherits from `BaseEppoClient`)
- Run `./gradlew spotlessApply` before pushing

---

## Requirement Coverage

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

---
*Roadmap created: 2026-08-18*

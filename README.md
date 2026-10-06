# Eppo JVM common SDK

[![Test and lint](https://github.com/Eppo-exp/sdk-common-jdk/actions/workflows/lint-test-sdk.yml/badge.svg)](https://github.com/Eppo-exp/sdk-common-jdk/actions/workflows/lint-test-sdk.yml)  
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cloud.eppo/sdk-common-jvm/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cloud.eppo/sdk-common-jvm)

This is the common SDK for the Eppo JVM SDKs. It provides a set of classes and interfaces that are used by the SDKs to
interact with the Eppo API. You should probably not use this library directly and instead use the [Android](https://github.com/Eppo-exp/android-sdk)
or [JVM](https://github.com/Eppo-exp/java-server-sdk) SDKs.

## Usage

### build.gradle:

```groovy
dependencies {
  implementation 'cloud.eppo:sdk-common-jvm:4.0.0'
}
```

## Releasing a new version

Publishing a stable GitHub release starts `publish-release.yml`. Every new release publishes a new version of
`sdk-common-jvm` and publishes `eppo-sdk-framework` first when its selected version is new.
The GitHub tag is the common version (`v4.0.0`); framework is independently versioned
(`0.1.0`). Maintainers choose both versions in Gradle; automation does not infer semver.

| GitHub tag | Common | Framework | Packages published |
|---|---|---|---|
| `v4.0.0` | `4.0.0` | `0.1.0` | Both, for the first release |
| `v4.0.1` | `4.0.1` | `0.1.0` | Common only |
| `v4.1.0` | `4.1.0` | `0.2.0` | Both |

### Prepare and publish

1. Commit and merge the stable common version in `eppo-sdk-common/build.gradle`.
   Set the root framework version in `build.gradle` to the exact stable version to
   publish or reuse. A framework change requires a new framework version and a new
   common version. Never leave the root version at SNAPSHOT for a stable common release.
2. In GitHub, open **Releases → Draft a new release**. Choose the reviewed commit
   containing those versions and create a tag matching the common version, such as
   `v4.0.1`. Write the release notes (or use GitHub's generated notes) and include
   the framework version being published or reused. No local release CLI is needed.
3. Review and publish the draft in GitHub. The workflow validates the tag, versions,
   clean checkout and main ancestry; tests the SDK; publishes framework if needed;
   waits for its artifacts; and publishes common. It then verifies package checksums
   and resolves common and the framework tests classifier in a separate consumer build
   using Central, without `mavenLocal` or project dependencies.
4. Check the [release workflow](https://github.com/Eppo-exp/sdk-common-jdk/actions/workflows/publish-release.yml)
   for completion. Publishing the GitHub release starts the process; it does not mean
   Central already serves the packages. Prereleases do not publish stable packages.
   Promoting an already-published prerelease does not start this workflow, and rerunning
   its skipped job replays the prerelease event. For a prerelease that has no publication
   records, delete the prerelease (keep its tag), then publish a full release on that tag.
   Never use this procedure on a release holding provenance for published packages.

### Reuse and retries

Before recording each new publication attempt, the workflow runs `jreleaserDeploy --dryrun`
to validate signing, POMs and configuration without uploading. A dry run does not validate
Central credentials or guarantee that the real deploy will succeed.
Before uploading a new package, the workflow attaches an immutable JSON intent record
to its GitHub release. It records source commit, build-input fingerprint and expected
file checksums. It is never replaced or copied when reusing a package. Completion is
determined by verifying the files on Central, not by rewriting the record’s state.
Framework reuse requires matching recorded inputs and verified Central files; changes
without a framework version bump fail. Inputs conservatively include root sources and
tests, shared Gradle/build settings, and the pinned fixture revision in
`scripts/release/test-data-ref`. All framework source-set files, including Markdown resources, are included.
Common-only sources and administrative documentation are excluded from the framework fingerprint.
The framework tests JAR references common classes `JacksonConfigurationParser`, `OkHttpEppoClient`,
`EppoModule`, and `EppoValueDeserializer`. A common-only change that still compiles but breaks
binary compatibility with those references is not detected by this fingerprint. Bump framework
too when making such a change, so downstream users get rebuilt test helpers.
Update that fixture pin deliberately;
changing it changes the published framework tests JAR and requires a framework bump.

Staging is cleared between packages. If framework succeeds and common fails, rerun the
same workflow: verified framework publication is reused and common resumes. A retry
of an existing release can skip packages already published by that release; each new
release must have a new common version. A publication attempt is recorded **before** uploading. If its files are still missing,
reruns wait up to 20 minutes and stop rather than uploading again. Before rerunning or
deleting anything, determine what happened to that attempt:

1. Open the workflow run identified by the record's `run` field. Reruns share that ID,
   so inspect every attempt's "Publish required packages and verify Central" log.
   Look for `uploaded as deployment` and the deployment ID that follows. Framework
   uploads precede `Verified eppo-sdk-framework:<version>`; common uploads follow it.
2. Find the deployment in the [Central Portal](https://central.sonatype.com/publishing/deployments)
   using an account with access to publish the `cloud.eppo` namespace.

| Deployment state / evidence | Action |
|---|---|
| PENDING or VALIDATING | Wait for the state to change. Keep the record. |
| PUBLISHING or PUBLISHED | Wait for Central to serve the files, then rerun to reuse them. |
| VALIDATED | Publish in the Portal and rerun, or drop the deployment and follow the FAILED recovery below. |
| FAILED | Inspect the error, drop the failed deployment, then remove its intent asset and rerun. Retain deployment evidence if contacting Sonatype support. |
| No deployment, and every attempt's log proves failure before any `Uploading` line for this package | No upload was attempted. Remove its intent asset, fix the cause and rerun. |
| An `Uploading` line without a returned deployment ID, missing/truncated logs, or any other ambiguity | The upload may have been accepted. Keep the record and inspect Portal deployments from that time; do not upload again until the outcome is established. |

Only after confirming that the attempt cannot publish and the package is not on Central,
remove its intent asset using `gh release delete-asset <tag> <artifact>-<version>.json -R Eppo-exp/sdk-common-jdk`.
Never delete a record while its deployment can still publish or its package is already on
Central. Never delete or recreate a GitHub release holding a record for a published
package: losing that provenance prevents future framework reuse. Missing deployment
history alone is not proof that nothing was published. The retained `attempting` state
records intent, not current deployment status. Coordinates without provenance,
conflicting checksums and HTTP errors fail closed.

After completion, development versions can return to SNAPSHOT. Before a later common-only
stable release, restore the exact published framework version in the root build. Framework
is always released with common; independent framework-only releases are not supported.

Release runs use a shared concurrency group with `queue: max`, so pending releases
queue instead of replacing one another (up to GitHub’s 100-run limit).

### Repository setup

Publishing the GitHub release authorizes the workflow to validate and publish the packages;
there is no additional environment approval or environment setup prerequisite.
The existing repository-level Maven/GPG secrets and `GPG_PUBLIC_KEY` variable remain
unchanged and are shared with snapshot publishing.

The workflow needs contents-write permission to attach publication records. Keep GitHub
immutable releases disabled for this repository: records are attached after publication,
and recovery of a failed attempt can require deleting an asset. Immutable releases prevent
that asset lifecycle. This workflow does not change repository settings. Release
publication must be initiated by a user or token that can trigger Actions, such as
publishing through the GitHub UI.

Release tooling tests run in CI and locally with `make test-release`. Snapshot publishing
keeps its separate workflow. The server SDK has its own downstream version and release.

## Using Snapshots

If you would like to live on the bleeding edge, you can try running against a snapshot build. Keep in mind that snapshots
represent the most recent changes on master and may contain bugs.

### build.gradle:

```groovy
repositories {
  maven {
    url "https://central.sonatype.com/repository/maven-snapshots/"
  }
}

dependencies {
  implementation 'cloud.eppo:sdk-common-jvm:X.Y.Z-SNAPSHOT'
}
```

### Publishing Snapshots

Snapshots are published automatically after each push to the `main` branch.

#### Publishing from an Unmerged Branch

To publish a snapshot from a branch that hasn't been merged to `main` yet (e.g., for testing in downstream SDKs):

1. Push your branch to the `snapshot/*` namespace:
   ```bash
   # From your feature branch
   git push origin HEAD:snapshot/my-feature

   # Or push an existing branch
   git push origin my-branch:snapshot/my-feature
   ```

2. This triggers the snapshot publish workflow, which will:
   - Run tests
   - Build and sign artifacts
   - Deploy to Maven Central Snapshots

3. Monitor the workflow at: [Actions > Publish SDK Snapshot](https://github.com/Eppo-exp/sdk-common-jdk/actions/workflows/publish-snapshot.yml)

4. Once published, use the snapshot in downstream projects by updating the version in `build.gradle`.

**Note:** The `snapshot/*` branch is only used to trigger the publish workflow. You can delete it after the snapshot is published:
```bash
git push origin --delete snapshot/my-feature
```

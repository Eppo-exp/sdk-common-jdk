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

### Reuse and retries

Before uploading a new package, the workflow attaches an immutable JSON intent record
to its GitHub release. It records source commit, build-input fingerprint and expected
file checksums. It is never replaced or copied when reusing a package. Completion is
determined by verifying the files on Central, not by rewriting the record’s state.
Framework reuse requires matching recorded inputs and verified Central files; changes
without a framework version bump fail. Inputs conservatively include root sources and
tests, shared Gradle/build settings, and the pinned fixture revision in
`scripts/release/test-data-ref`. All framework source-set files, including Markdown resources, are included.
Common-only sources and administrative documentation are excluded from the framework fingerprint. Update that fixture pin deliberately;
changing it changes the published framework tests JAR and requires a framework bump.

Staging is cleared between packages. If framework succeeds and common fails, rerun the
same workflow: verified framework publication is reused and common resumes. A retry
of an existing release can skip packages already published by that release; each new
release must have a new common version. A publication
attempt is recorded **before** uploading. If its files are still missing, reruns wait up
to 20 minutes and stop rather than submitting another upload. Inspect the existing
Central Portal deployment and finish it there, then rerun. If an upload never reached
Central or was rejected, confirm that it cannot publish before removing its intent JSON asset and rerunning.
The retained `attempting` state records intent, not current deployment status; check
Central before any manual deletion. Never delete a record for a published package. Coordinates
without provenance, conflicting checksums and HTTP errors fail closed.

After completion, development versions can return to SNAPSHOT. Before a later common-only
stable release, restore the exact published framework version in the root build. Framework
is always released with common; independent framework-only releases are not supported.

Release runs use a shared concurrency group with `queue: max`, so pending releases
queue instead of replacing one another (up to GitHub’s 100-run limit).

### Repository setup

Before the first release, a repository admin must create `maven-central-release` with
release owners as required reviewers and an allowed **tag** rule matching `v*`.
The workflow references this environment as an approval gate: the publish job waits
for approval before any of its steps run. A main-branch-only rule will block release
events, whose ref is a tag; the workflow separately validates the exact version and
main ancestry. GitHub automatically creates a missing environment without protection
rules, so referencing its name alone does not enable approval. This setup is a separate
repository-admin prerequisite, not a configuration change performed by this PR.

Keep the existing repository-level `MAVEN_CENTRAL_TOKEN_USERNAME`,
`MAVEN_CENTRAL_TOKEN_PASSWORD`, `GPG_PASSPHRASE`, and `GPG_PRIVATE_KEY` secrets and
`GPG_PUBLIC_KEY` variable unchanged. Do not move or duplicate them into the environment.
Both release and snapshot publishing use this existing configuration; the snapshot
workflow has no environment. The approval gate protects this release job from accidental
publication; it does not isolate repository secrets from users who can edit workflows.

The workflow needs contents-write permission to attach publication records. Release
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

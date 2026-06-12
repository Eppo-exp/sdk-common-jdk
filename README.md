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

Releases are published to Maven Central via GitHub Actions. There are two artifacts, each with its own workflow:

| Artifact | artifactId | Workflow |
|---|---|---|
| Framework SDK | `eppo-sdk-framework` | `publish-framework.yml` |
| Common SDK | `sdk-common-jvm` | `publish-common.yml` |

### Steps

> **Ordering requirement:** If releasing both artifacts, release `eppo-sdk-framework` first and confirm it is visible on Maven Central before triggering `publish-common.yml`. The `sdk-common-jvm` POM declares `eppo-sdk-framework` as a compile dependency; releasing common first leaves consumers with an unresolvable transitive dependency.

1. Bump the version in the relevant `build.gradle` (root for framework, `eppo-sdk-common/build.gradle` for common) — drop the `-SNAPSHOT` suffix
2. Merge the version bump to `main`
3. Trigger the workflow via the GitHub UI or CLI:

```bash
# Release the Framework SDK (do this first if releasing both)
gh workflow run publish-framework.yml --ref main --field version=0.1.0

# Release the Common SDK
gh workflow run publish-common.yml --ref main --field version=4.0.0
```

The workflow will:
- Verify the version in `build.gradle` matches the input
- Run all tests
- Sign and publish the artifact to Maven Central
- Create and push a tag (`eppo-sdk-framework-vN.N.N` or `sdk-common-jvm-vN.N.N`) on successful deploy

4. After the release is confirmed on Maven Central, bump `main` back to the next `-SNAPSHOT` version (e.g. `4.0.1-SNAPSHOT` / `0.1.1-SNAPSHOT`) so that snapshot publishing continues to work.

Monitor progress at [GitHub Actions](https://github.com/Eppo-exp/sdk-common-jdk/actions).

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

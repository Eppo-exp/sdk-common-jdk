# Eppo JVM common SDK

This is the common SDK for the Eppo JVM SDKs. It provides a set of classes and interfaces that are used by the SDKs to
interact with the Eppo API. You should probably not use this library directly and instead use the [Android](https://github.com/Eppo-exp/android-sdk)
or [JVM](https://github.com/Eppo-exp/java-server-sdk) SDKs.

## Usage

### build.gradle:

```groovy
dependencies {
  implementation 'cloud.eppo:sdk-common-jvm:3.5.2'
}
```

## Releasing a new version

For publishing a release locally, follow the steps below.

### Prerequisites

1. [Generate a user token](https://central.sonatype.org/publish/generate-token/) on `s01.oss.sonatype.org`;
2. [Configure a GPG key](https://central.sonatype.org/publish/requirements/gpg/) for signing the artifact. Don't forget to upload it to the key server;
3. Make sure you have the following vars in your `~/.gradle/gradle.properties` file:
   1. `ossrhUsername` - User token username for Sonatype generated in step 1
   2. `ossrhPassword` - User token password for Sonatype generated in step 1
   3. `signing.keyId` - GPG key ID generated in step 2
   4. `signing.password` - GPG key password generated in step 2
   5. `signing.secretKeyRingFile` - Path to GPG key file generated in step 2

Once you have the prerequisites, follow the steps below to release a new version:

1. Bump the project version in `build.gradle`
2. Run `./gradlew publish`
3. Follow the steps in [this page](https://central.sonatype.org/publish/release/#credentials) to promote your release

## Using Snapshots

If you would like to live on the bleeding edge, you can try running against a snapshot build. Keep in mind that snapshots
represent the most recent changes on master and may contain bugs.
Snapshots are published automatically after each push to `main` branch.

### build.gradle:

```groovy
repositories {
  maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots" }
}

dependencies {
  implementation 'cloud.eppo:sdk-common-jvm:3.4.2-SNAPSHOT'
}
```

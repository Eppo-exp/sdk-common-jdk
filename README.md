# Eppo JVM common SDK

This is the common SDK for the Eppo JVM SDKs. It provides a set of classes and interfaces that are used by the SDKs to
interact with the Eppo API. You should probably not use this library directly and instead use the [Android](https://github.com/Eppo-exp/android-sdk)
or [JVM](https://github.com/Eppo-exp/java-server-sdk) SDKs.

## Usage

### build.gradle:

```groovy
dependencies {
  implementation 'cloud.eppo:sdk-common-jvm:1.0.0'
}
```

## Releasing a new version

For publishing a release locally, follow the steps below.

You haven't yet, generate a user token on `s01.oss.sonatype.org` and add it to your `~/.gradle/gradle.properties` file.
Also make sure you have a configured GPG key for signing the artifact.

1. Make sure you have the following vars in your `~/.gradle/gradle.properties` file:
    1.1. `ossrhUsername` - User token username for Sonatype
    1.2. `ossrhPassword` - User token password for Sonatype
    1.3. `signing.keyId` - GPG key ID
    1.4. `signing.password` - GPG key password
    1.4. `signing.secretKeyRingFile` - Path to GPG key file
2. Bump the project version in `build.gradle`
3. Run `./gradlew publish`
4. Follow the steps in [this page](https://central.sonatype.org/publish/release/#credentials) to promote your release

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
  implementation 'cloud.eppo:sdk-common-jvm:1.0.0-SNAPSHOT'
}
```

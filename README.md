# Eppo JVM common SDK

This is the common SDK for the Eppo JVM SDKs. It provides a set of classes and interfaces that are used by the SDKs to
interact with the Eppo API. You should probably not use this library directly and instead use the [Android](https://github.com/Eppo-exp/android-sdk)
or [JVM](https://github.com/Eppo-exp/java-server-sdk) SDKs.

# Usage

## build.gradle:

```groovy
dependencies {
  implementation 'com.eppo:eppo-jvm-common-sdk:1.0.0'
}
```

# Releasing a new version

Bump the project version in `build.gradle`
To release a new version of the SDK, you need to create a new tag in the repository. The tag should be named `vX.Y.Z`,
where `X.Y.Z` is the version number of the release. For example, if you are releasing version 1.2.3, the tag should be
named `v1.2.3`.

# Using Snapshots
If you would like to live on the bleeding edge, you can try running against a snapshot build. Keep in mind that snapshots
represent the most recent changes on master and may contain bugs.

## build.gradle:

```groovy
repositories {
  maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
}

dependencies {
  implementation 'com.eppo:eppo-jvm-common-sdk:1.0.0-SNAPSHOT'
}
```

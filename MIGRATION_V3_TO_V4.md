# Migration Guide: v3.x to v4.0

## Breaking Change: OkHttp is now a Peer Dependency

### What Changed

In v4.0, OkHttp is no longer bundled with the SDK. Instead, it's a **peer dependency** that your application must provide.

**Why this change?**
- **Flexibility**: Your application controls which OkHttp version to use (4.12+, 5.x, or any compatible version)
- **Smaller SDK**: ~2MB reduction (no bundled OkHttp, Okio, and Kotlin stdlib)
- **Avoid conflicts**: Eliminates OkHttp version conflicts between SDK and your application

---

### Migration Steps

**Simply add OkHttp to your dependencies. No code changes required.**

#### Gradle

```gradle
dependencies {
  implementation 'cloud.eppo:sdk-common-jvm:4.0.0'
  implementation 'com.squareup.okhttp3:okhttp:4.12.0'  // ADD THIS LINE
}
```

**Or use OkHttp 5.x:**
```gradle
dependencies {
  implementation 'cloud.eppo:sdk-common-jvm:4.0.0'
  implementation 'com.squareup.okhttp3:okhttp:5.0.0-alpha.14'  // 5.x works too!
}
```

#### Maven

```xml
<dependencies>
  <dependency>
    <groupId>cloud.eppo</groupId>
    <artifactId>sdk-common-jvm</artifactId>
    <version>4.0.0</version>
  </dependency>
  <!-- ADD THIS -->
  <dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>  <!-- Or 5.0.0-alpha.14 for 5.x -->
  </dependency>
</dependencies>
```

**That's it!** Your existing application code continues to work without modification.

---

### Supported OkHttp Versions

- ✅ **OkHttp 4.12+** - Fully tested and supported
- ✅ **OkHttp 5.x** - Compatible and tested (tested with 5.0.0-alpha.14)
- ⚠️ **OkHttp < 4.12** - Not officially supported (may work but untested)

The SDK uses only stable OkHttp APIs that are compatible across both 4.x and 5.x versions.

---

### Before and After

#### Before v4.0 (OkHttp Bundled)
```
Your Application
└── eppo-sdk:3.x
    └── okhttp:4.12.0 (bundled by SDK)
        ├── okio:2.x
        └── kotlin-stdlib

❌ Problem: If your app also uses okhttp:4.10.0, you have TWO versions on the classpath!
```

#### After v4.0 (OkHttp as Peer Dependency)
```
Your Application
├── eppo-sdk:4.0.0
└── okhttp:4.12.0 (your choice - 4.12+, 5.x, whatever you need)
    ├── okio:2.x
    └── kotlin-stdlib

✅ Benefit: ONE version of OkHttp, controlled by your application
```

---

### Troubleshooting

#### Error: `NoClassDefFoundError: okhttp3/OkHttpClient`

**Cause:** OkHttp is not on your classpath.

**Solution:** Add OkHttp to your dependencies:
```gradle
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

---

#### I'm already using OkHttp in my application

**Great!** The SDK will use whatever version you have (as long as it's 4.12+ or 5.x). No changes needed.

**Example:**
```gradle
dependencies {
  implementation 'com.squareup.okhttp3:okhttp:4.10.0'  // Your existing version
  implementation 'cloud.eppo:sdk-common-jvm:4.0.0'     // Upgrade to v4
}
```

**Action:** Upgrade your OkHttp to 4.12.0+ if you're on an older version:
```gradle
dependencies {
  implementation 'com.squareup.okhttp3:okhttp:4.12.0'  // Upgrade this
  implementation 'cloud.eppo:sdk-common-jvm:4.0.0'
}
```

---

#### I want to use OkHttp 5.x

**Fully supported!** Just use 5.x in your dependencies:

```gradle
dependencies {
  implementation 'com.squareup.okhttp3:okhttp:5.0.0-alpha.14'
  implementation 'cloud.eppo:sdk-common-jvm:4.0.0'
}
```

The SDK has been tested with both OkHttp 4.12.0 and 5.0.0-alpha.14.

---

### What Didn't Change

✅ All SDK APIs remain the same
✅ No code changes required in your application
✅ EppoClient usage unchanged
✅ Assignment methods unchanged
✅ Configuration and polling behavior unchanged

**Only change:** You must explicitly declare OkHttp in your dependencies.

---

### Rollback

If you need to temporarily rollback to v3.x:

```gradle
dependencies {
  implementation 'cloud.eppo:sdk-common-jvm:3.13.2'  // No OkHttp dependency needed
}
```

---

### Need Help?

If you encounter issues during migration:
- Review the examples in this guide
- Check that you're using OkHttp 4.12+ or 5.x
- Open an issue at https://github.com/Eppo-exp/sdk-common-jvm/issues

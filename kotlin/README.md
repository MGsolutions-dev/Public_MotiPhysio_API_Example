# Kotlin Sample — Public Moti-Physio API

## Requirements

- Kotlin 1.8+ / JVM 11+
- OkHttp 4.x
- Gson 2.x
- Kotlin Coroutines

## Setup

### Gradle (recommended)

Add to `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
```

Or `build.gradle`:

```groovy
dependencies {
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
}
```

### Edit credentials

Open `MotiPhysioClient.kt` and edit the three constants at the top:

```kotlin
private const val BASE_URL     = "https://api.motiphysio.com"
private const val PROGRAM_ID   = "YOUR_PROGRAM_ID"
private const val SECURITY_KEY = "YOUR_SECURITY_KEY"
```

## Run (JVM / command-line)

```bash
# With Gradle
./gradlew run

# Manual compile (requires jars on classpath)
kotlinc MotiPhysioClient.kt -include-runtime \
    -cp okhttp-4.12.0.jar:gson-2.10.1.jar:kotlinx-coroutines-core-1.7.3.jar \
    -d moti.jar
java -cp moti.jar:okhttp-4.12.0.jar:gson-2.10.1.jar:kotlinx-coroutines-core-1.7.3.jar \
    MotiPhysioClientKt
```

## Use in an Android App

1. Copy `MotiPhysioClient.kt` (and the data classes) into your Android project.
2. Remove the top-level `fun main()` block.
3. Call from a ViewModel using `viewModelScope.launch { }`:

```kotlin
viewModelScope.launch {
    try {
        val users = motiPhysioClient.getUserList()
        // update LiveData / StateFlow with users
    } catch (e: ApiException) {
        // handle API error
    }
}
```

> **Note:** Make sure `INTERNET` permission is declared in `AndroidManifest.xml`.

## File Structure

```
kotlin/
├── MotiPhysioClient.kt   # All 8 endpoints, data classes, coroutines
└── README.md
```

## Notes

- All network calls run on `Dispatchers.IO` — safe for coroutines and Android.
- Business-logic errors (`"error"` key in body) are thrown as `ApiException`.
- A 200 ms `delay()` is added between requests to stay within the 10 req/s rate limit.
- In production, use Retrofit + a Gson/Moshi converter for cleaner API layer separation.

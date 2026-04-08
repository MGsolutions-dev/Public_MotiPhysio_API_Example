# Java Sample — Public Moti-Physio API

## Requirements

- Java 11 or later (uses `java.net.http.HttpClient`)
- [org.json](https://mvnrepository.com/artifact/org.json/json) for JSON parsing

### Option A — Download the JAR manually

1. Download `json-20231013.jar` from Maven Central.
2. Place it in the `java/` folder.

Compile and run:
```bash
javac -cp .:json-20231013.jar MotiPhysioClient.java
java  -cp .:json-20231013.jar MotiPhysioClient
```
(On Windows replace `:` with `;`)

### Option B — Maven project

Add to your `pom.xml`:
```xml
<dependency>
  <groupId>org.json</groupId>
  <artifactId>json</artifactId>
  <version>20231013</version>
</dependency>
```

### Option C — Gradle project

Add to your `build.gradle`:
```groovy
implementation 'org.json:json:20231013'
```

## Setup

Open `MotiPhysioClient.java` and edit the three constants at the top:

```java
private static final String BASE_URL     = "https://api.motiphysio.com";
private static final String PROGRAM_ID   = "YOUR_PROGRAM_ID";
private static final String SECURITY_KEY = "YOUR_SECURITY_KEY";
```

## File Structure

```
java/
├── MotiPhysioClient.java   # All 8 endpoints, zero external HTTP dependency
└── README.md
```

## Notes

- Uses the built-in `java.net.http.HttpClient` — no third-party HTTP library needed.
- A 200 ms sleep is added between requests to stay within the 10 req/s rate limit.
- Business-logic errors (`"error"` key in response body) are thrown as `RuntimeException`.
- For production use consider wrapping the calls in a dedicated service class and
  using Gson or Jackson for type-safe JSON deserialization.

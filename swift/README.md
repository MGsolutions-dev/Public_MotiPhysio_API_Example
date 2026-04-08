# Swift Sample — Public Moti-Physio API

## Requirements

- Swift 5.5+ (Xcode 13+ or Swift toolchain on Linux)
- iOS 15+ / macOS 12+ (for `async/await` and `URLSession` async APIs)
- No external dependencies — Foundation only

## Setup

Open `MotiPhysioClient.swift` and edit the three constants at the top:

```swift
let BASE_URL:     String = "https://api.motiphysio.com"
let PROGRAM_ID:   String = "YOUR_PROGRAM_ID"
let SECURITY_KEY: String = "YOUR_SECURITY_KEY"
```

## Run (macOS command-line)

```bash
swiftc MotiPhysioClient.swift -o motiPhysio
./motiPhysio
```

## Use in an iOS / macOS App (Xcode)

1. Remove the `@main struct Main` block at the bottom of the file.
2. Copy `MotiPhysioClient` and all model types into your Xcode project.
3. Call the async methods from a `Task { }` block or a Swift concurrency context:

```swift
Task {
    do {
        let client = MotiPhysioClient()
        let users = try await client.getUserList()
        print(users)
    } catch {
        print(error)
    }
}
```

## File Structure

```
swift/
├── MotiPhysioClient.swift   # All 8 endpoints, Codable models, async/await
└── README.md
```

## Notes

- Uses `URLSession` with `async/await` — no Combine or callbacks needed.
- All response models are `Decodable` structs for type safety.
- Business-logic errors (`"error"` key in body) are thrown as `APIError.apiError`.
- A 200 ms `Task.sleep` is added between requests to stay within the rate limit.

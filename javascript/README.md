# JavaScript (Node.js) Sample — Public Moti-Physio API

## Requirements

- Node.js 14+
- npm

## Setup

```bash
# Install dependencies
npm install
```

Open `motiPhysioClient.js` and edit the three constants at the top:

```js
const BASE_URL     = "https://api.motiphysio.com";
const PROGRAM_ID   = "YOUR_PROGRAM_ID";
const SECURITY_KEY = "YOUR_SECURITY_KEY";
```

## Run

```bash
node motiPhysioClient.js
# or
npm start
```

## File Structure

```
javascript/
├── motiPhysioClient.js   # All 8 endpoints, async/await pattern
├── package.json
└── README.md
```

## Notes

- Uses `axios` for HTTP requests (supports timeout, error handling out of the box).
- All functions are `async` — wrap calls in a `try/catch` block in production code.
- A 200 ms `sleep()` is added between requests to stay within the 10 req/s rate limit.
- Business-logic errors (`"error"` key in response body) are thrown as `Error` objects.

# Python Sample — Public Moti-Physio API

## Requirements

- Python 3.8+
- `requests` library

```bash
pip install requests
```

## Setup

Open `moti_physio_client.py` and edit the three constants at the top:

```python
BASE_URL     = "https://api.motiphysio.com"
PROGRAM_ID   = "YOUR_PROGRAM_ID"
SECURITY_KEY = "YOUR_SECURITY_KEY"
```

## Run

```bash
python moti_physio_client.py
```

## File Structure

```
python/
└── moti_physio_client.py   # All 8 endpoints, clean helper functions
```

## What the sample does

1. Fetches the full member list for your program
2. Fetches the detailed profile of the first member in the list
3. Fetches Static / OHS / OLS analysis record lists for that member
4. Fetches Presigned Report URLs for each analysis type (first record only)

All responses are printed to the console so you can verify the shape of
the data before building your own integration logic.

## Notes

- A 200 ms delay is added between requests to stay well within the
  10 req/s rate limit.
- Business-logic errors (`"error"` key in the response body) are raised
  as `ValueError` by the `post()` helper so you can catch them explicitly.

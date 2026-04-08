# Public Moti-Physio API — Sample Code

Welcome to the **Public Moti-Physio API** sample code repository.  
This repository provides ready-to-run example code in multiple programming languages so your team can integrate with the Moti-Physio platform as quickly as possible.

---

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Authentication](#authentication)
- [API Endpoints Summary](#api-endpoints-summary)
- [Rate Limiting](#rate-limiting)
- [Error Handling](#error-handling)
- [Sample Code by Language](#sample-code-by-language)

---

## Overview

The **Public Moti-Physio API** is a REST API that gives external partners access to rehabilitation and exercise analysis data collected by the Moti-Physio system.

| Item | Detail |
|------|--------|
| Base URL | `https://api.motiphysio.com` |
| Protocol | HTTP/HTTPS |
| Method | All endpoints use `POST` |
| Content-Type | `application/json` |
| Authentication | `program_id` + `security_key` in request body |
| API Version | `/v1` prefix on all endpoints |

> **Interactive API Docs**  
> Once the server is running you can explore all endpoints at:  
> - Swagger UI → `https://api.motiphysio.com/docs`  
> - ReDoc → `https://api.motiphysio.com/redoc`

---

## Quick Start

1. Obtain your `program_id` and `security_key` from the Moti-Physio team.
2. Choose a sample from the language folder that matches your stack.
3. Replace `BASE_URL`, `PROGRAM_ID`, and `SECURITY_KEY` with your real values.
4. Run the sample and verify you receive a successful response.

---

## Authentication

Every request **must** include `program_id` and `security_key` in the JSON body.  
There is no header-based token; authentication is entirely body-based.

```json
{
  "program_id": "YOUR_PROGRAM_ID",
  "security_key": "YOUR_SECURITY_KEY"
}
```

If the `security_key` does not match the server's record, every endpoint returns:

```json
{"error": "password is not match"}
```

---

## API Endpoints Summary

All endpoints use the `POST` method and are prefixed with `/v1`.

| # | Endpoint | Description |
|---|----------|-------------|
| 1 | `POST /v1/get_user_list` | Get the list of all members in your program |
| 2 | `POST /v1/get_user_info` | Get detailed info for a specific member |
| 3 | `POST /v1/get_user_static_analysis_list` | Get the Static (posture) analysis record list |
| 4 | `POST /v1/get_user_ohs_analysis_list` | Get the OHS (core function) analysis record list |
| 5 | `POST /v1/get_user_ols_analysis_list` | Get the OLS (balance) analysis record list |
| 6 | `POST /v1/get_user_static_analysis_report` | Get Presigned URLs for Static analysis report images |
| 7 | `POST /v1/get_user_ohs_analysis_report` | Get Presigned URLs for OHS report images |
| 8 | `POST /v1/get_user_ols_analysis_report` | Get Presigned URLs for OLS report images |

### Endpoint Details

#### 1. `POST /v1/get_user_list`
Returns every member registered under your `program_id`.  
Optionally filter by registration date range using Unix timestamps.

**Request body**
```json
{
  "program_id": "1489",
  "security_key": "YOUR_KEY",
  "start_period": 1700000000,
  "end_period":   1800000000
}
```
`start_period` / `end_period` are **optional**.

**Response**
```json
[
  {
    "user_id": "1489-00001",
    "name": "Hong Gil-dong",
    "birth": "1990-01-01",
    "gender": "M",
    "register_date": 1700000000
  }
]
```

---

#### 2. `POST /v1/get_user_info`
Returns detailed profile information for a single member.

**Request body**
```json
{
  "program_id": "1489",
  "security_key": "YOUR_KEY",
  "user_id": "1489-00001"
}
```

**Response**
```json
{
  "user_id": "1489-00001",
  "name": "Hong Gil-dong",
  "birth": "1990-01-01",
  "gender": "M",
  "height": 175.0,
  "weight": 70.0,
  "register_date": 1700000000
}
```

---

#### 3–5. Analysis List Endpoints
All three analysis list endpoints share the same request/response shape.

**Request body**
```json
{
  "program_id": "1489",
  "security_key": "YOUR_KEY",
  "user_id": "1489-00001",
  "start_period": 1700000000,
  "end_period":   1800000000
}
```
`start_period` / `end_period` are **optional**.

**Response**
```json
[
  {
    "index": 0,
    "measurement_time": 1700000000,
    "version": "2.0"
  }
]
```
> The `index` field is the `analysis_index` you pass to the report endpoints (6–8).  
> Indices are **0-based** and ordered by measurement time.

---

#### 6–8. Analysis Report Endpoints
Returns **Presigned Download URLs** for report images stored in AWS S3.  
URLs are valid for **24 hours** and require no further authentication to download.

**Request body**
```json
{
  "program_id": "1489",
  "security_key": "YOUR_KEY",
  "user_id": "1489-00001",
  "analysis_index": 0
}
```

**Response (Static example)**
```json
{
  "user_id": "1489-00001",
  "analysis_index": 0,
  "reports": {
    "skeleton_result_sheet":         [{"page_index": 0, "filename": "...", "presigned_url": "https://..."}],
    "expert_result_sheet":           [{"page_index": 0, "filename": "...", "presigned_url": "https://..."}],
    "original_image_result_sheet":   [{"page_index": 0, "filename": "...", "presigned_url": "https://..."}],
    "original_image":                [{"page_index": 0, "filename": "...", "presigned_url": "https://..."}],
    "risk_ranking_result_sheet":     [{"page_index": 0, "filename": "...", "presigned_url": "https://..."}]
  },
  "url_expiration_seconds": 86400
}
```

---

## Rate Limiting

The server enforces a **sliding-window rate limit**:

| Limit | Window |
|-------|--------|
| 10 requests | 1 second per IP |

If you exceed the limit, the server returns HTTP **429** with:

```json
{"error": "Rate limit exceeded. You are allowed up to 10 requests per second per IP. Please slow down and try again."}
```

**Best practice:** add a small delay (e.g. 100–200 ms) between consecutive requests in batch operations.

---

## Error Handling

All errors are returned as JSON with an `"error"` key:

| HTTP Status | `error` value | Cause |
|-------------|---------------|-------|
| 200 | `"password is not match"` | Invalid `security_key` |
| 200 | `"No data"` | No records found |
| 200 | `"Invalid index. Valid range: 0-N"` | `analysis_index` out of range |
| 200 | `"No report files found"` | No report images in S3 |
| 200 | `"Database error: ..."` | MySQL error |
| 429 | `"Rate limit exceeded. ..."` | Too many requests |
| 500 | `"Server error: ..."` | Unexpected server error |

> Note: business-logic errors (wrong key, no data, bad index) use HTTP **200** with an `"error"` field in the body. Always check for the `"error"` key before processing the response.

---

## Sample Code by Language

| Language | Folder | Highlights |
|----------|--------|------------|
| Python | [`python/`](./python/) | `requests` library, clean class-based client |
| JavaScript (Node.js) | [`javascript/`](./javascript/) | `axios` library, async/await pattern |
| Java | [`java/`](./java/) | `HttpClient` (Java 11+), no external dependencies |
| Swift | [`swift/`](./swift/) | `URLSession`, async/await (iOS 15+ / macOS 12+) |
| Kotlin | [`kotlin/`](./kotlin/) | `OkHttp` + coroutines, Android-ready |

Each folder contains:
- A self-contained sample file with **all 8 endpoints** demonstrated
- A `README.md` explaining how to run the sample

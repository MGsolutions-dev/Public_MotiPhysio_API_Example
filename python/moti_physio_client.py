"""
moti_physio_client.py
=====================
Public Moti-Physio API — Python Sample Client

This file demonstrates how to call all 8 endpoints of the
Public Moti-Physio API using the popular `requests` library.

Requirements:
    pip install requests

Usage:
    1. Set BASE_URL, PROGRAM_ID, SECURITY_KEY below.
    2. Run:  python moti_physio_client.py
"""

import time
import requests  # pip install requests

# ---------------------------------------------------------------------------
# Configuration — replace these values with your own credentials
# ---------------------------------------------------------------------------
BASE_URL    = "https://api.motiphysio.com"
PROGRAM_ID  = "YOUR_PROGRAM_ID"               # e.g. "1489"
SECURITY_KEY = "YOUR_SECURITY_KEY"            # e.g. "b25klhxkp36v6fcx7qkz"

# Sample user to use for the analysis / report examples
SAMPLE_USER_ID = "1489-00001"


# ---------------------------------------------------------------------------
# Helper: send a POST request and return the parsed JSON response
# ---------------------------------------------------------------------------
def post(endpoint: str, payload: dict) -> dict | list:
    """
    Sends a POST request to the given endpoint and returns the JSON response.

    Args:
        endpoint: Path after the base URL, e.g. "/v1/get_user_list"
        payload:  Dictionary that will be serialized to JSON in the request body

    Returns:
        Parsed JSON response (dict or list)

    Raises:
        requests.HTTPError: on HTTP 4xx/5xx responses
        ValueError:         if the server returns an error payload
    """
    url = BASE_URL + endpoint
    response = requests.post(url, json=payload, timeout=30)

    # Raise an exception for HTTP-level errors (4xx, 5xx)
    response.raise_for_status()

    data = response.json()

    # The API signals business-logic errors with an "error" key in the body
    # (using HTTP 200), so we check for that explicitly.
    if isinstance(data, dict) and "error" in data:
        raise ValueError(f"API error: {data['error']}")

    return data


# ---------------------------------------------------------------------------
# 1. get_user_list
#    Returns the list of all members registered under your program.
#    start_period / end_period are optional Unix timestamps for date filtering.
# ---------------------------------------------------------------------------
def get_user_list(start_period: int = None, end_period: int = None) -> list:
    """
    Fetch the member list for your program.

    Args:
        start_period: Optional. Filter members registered on or after this
                      Unix timestamp (seconds).
        end_period:   Optional. Filter members registered on or before this
                      Unix timestamp (seconds).

    Returns:
        List of member dicts, each containing:
          - user_id (str)
          - name    (str)
          - birth   (str, "YYYY-MM-DD")
          - gender  (str, "M" or "F")
          - register_date (int, Unix timestamp)
    """
    payload = {
        "program_id":   PROGRAM_ID,
        "security_key": SECURITY_KEY,
    }
    # Only include optional fields when they are explicitly provided
    if start_period is not None:
        payload["start_period"] = start_period
    if end_period is not None:
        payload["end_period"] = end_period

    return post("/v1/get_user_list", payload)


# ---------------------------------------------------------------------------
# 2. get_user_info
#    Returns detailed profile information for a single member.
# ---------------------------------------------------------------------------
def get_user_info(user_id: str) -> dict:
    """
    Fetch the full profile of a specific member.

    Args:
        user_id: The member's unique ID (e.g. "1489-00001").

    Returns:
        Dict containing:
          - user_id (str)
          - name    (str)
          - birth   (str)
          - gender  (str)
          - height  (float, cm)
          - weight  (float, kg)
          - register_date (int, Unix timestamp)
    """
    payload = {
        "program_id":   PROGRAM_ID,
        "security_key": SECURITY_KEY,
        "user_id":      user_id,
    }
    return post("/v1/get_user_info", payload)


# ---------------------------------------------------------------------------
# 3. get_user_static_analysis_list
#    Returns the Static (posture) analysis records for a member.
#    The "index" field in each record is the analysis_index you will pass
#    to get_user_static_analysis_report.
# ---------------------------------------------------------------------------
def get_user_static_analysis_list(
    user_id: str,
    start_period: int = None,
    end_period: int = None,
) -> list:
    """
    Fetch the list of Static (posture) analysis sessions for a member.

    Args:
        user_id:      Member ID.
        start_period: Optional Unix timestamp lower bound.
        end_period:   Optional Unix timestamp upper bound.

    Returns:
        List of dicts, each containing:
          - index            (int, 0-based)
          - measurement_time (int, Unix timestamp)
          - version          (str)
    """
    payload = {
        "program_id":   PROGRAM_ID,
        "security_key": SECURITY_KEY,
        "user_id":      user_id,
    }
    if start_period is not None:
        payload["start_period"] = start_period
    if end_period is not None:
        payload["end_period"] = end_period

    return post("/v1/get_user_static_analysis_list", payload)


# ---------------------------------------------------------------------------
# 4. get_user_ohs_analysis_list
#    Returns the OHS (Overhead Squat / core function) analysis records.
# ---------------------------------------------------------------------------
def get_user_ohs_analysis_list(
    user_id: str,
    start_period: int = None,
    end_period: int = None,
) -> list:
    """
    Fetch the list of OHS (core function) analysis sessions for a member.

    Args / Returns: same structure as get_user_static_analysis_list.
    """
    payload = {
        "program_id":   PROGRAM_ID,
        "security_key": SECURITY_KEY,
        "user_id":      user_id,
    }
    if start_period is not None:
        payload["start_period"] = start_period
    if end_period is not None:
        payload["end_period"] = end_period

    return post("/v1/get_user_ohs_analysis_list", payload)


# ---------------------------------------------------------------------------
# 5. get_user_ols_analysis_list
#    Returns the OLS (One-Leg Stand / balance) analysis records.
# ---------------------------------------------------------------------------
def get_user_ols_analysis_list(
    user_id: str,
    start_period: int = None,
    end_period: int = None,
) -> list:
    """
    Fetch the list of OLS (balance) analysis sessions for a member.

    Args / Returns: same structure as get_user_static_analysis_list.
    """
    payload = {
        "program_id":   PROGRAM_ID,
        "security_key": SECURITY_KEY,
        "user_id":      user_id,
    }
    if start_period is not None:
        payload["start_period"] = start_period
    if end_period is not None:
        payload["end_period"] = end_period

    return post("/v1/get_user_ols_analysis_list", payload)


# ---------------------------------------------------------------------------
# 6. get_user_static_analysis_report
#    Returns Presigned S3 URLs for all Static analysis report images.
#    URLs expire after 24 hours.
# ---------------------------------------------------------------------------
def get_user_static_analysis_report(user_id: str, analysis_index: int) -> dict:
    """
    Get Presigned Download URLs for a Static analysis report.

    Args:
        user_id:        Member ID.
        analysis_index: 0-based index from get_user_static_analysis_list.

    Returns:
        Dict with keys:
          - user_id              (str)
          - analysis_index       (int)
          - url_expiration_seconds (int, 86400 = 24 h)
          - reports:
              skeleton_result_sheet       → list of page URL objects
              expert_result_sheet         → list of page URL objects
              original_image_result_sheet → list of page URL objects
              original_image              → list of page URL objects
              risk_ranking_result_sheet   → list of page URL objects

        Each URL object: {"page_index": int, "filename": str, "presigned_url": str}
    """
    payload = {
        "program_id":     PROGRAM_ID,
        "security_key":   SECURITY_KEY,
        "user_id":        user_id,
        "analysis_index": analysis_index,
    }
    return post("/v1/get_user_static_analysis_report", payload)


# ---------------------------------------------------------------------------
# 7. get_user_ohs_analysis_report
#    Returns Presigned S3 URLs for OHS report images.
# ---------------------------------------------------------------------------
def get_user_ohs_analysis_report(user_id: str, analysis_index: int) -> dict:
    """
    Get Presigned Download URLs for an OHS analysis report.

    Args:
        user_id:        Member ID.
        analysis_index: 0-based index from get_user_ohs_analysis_list.

    Returns:
        Dict with reports.ohs_result_sheet → list of page URL objects.
    """
    payload = {
        "program_id":     PROGRAM_ID,
        "security_key":   SECURITY_KEY,
        "user_id":        user_id,
        "analysis_index": analysis_index,
    }
    return post("/v1/get_user_ohs_analysis_report", payload)


# ---------------------------------------------------------------------------
# 8. get_user_ols_analysis_report
#    Returns Presigned S3 URLs for OLS report images (Left & Right).
# ---------------------------------------------------------------------------
def get_user_ols_analysis_report(user_id: str, analysis_index: int) -> dict:
    """
    Get Presigned Download URLs for an OLS analysis report.

    Args:
        user_id:        Member ID.
        analysis_index: 0-based index from get_user_ols_analysis_list.

    Returns:
        Dict with reports.ols_result_sheet → list of page URL objects.
        The list covers both Left and Right leg results.
    """
    payload = {
        "program_id":     PROGRAM_ID,
        "security_key":   SECURITY_KEY,
        "user_id":        user_id,
        "analysis_index": analysis_index,
    }
    return post("/v1/get_user_ols_analysis_report", payload)


# ---------------------------------------------------------------------------
# Main — demonstrates all 8 endpoints in sequence
# ---------------------------------------------------------------------------
def main():
    print("=" * 60)
    print("  Public Moti-Physio API — Python Sample")
    print("=" * 60)

    # ------------------------------------------------------------------
    # 1. Member list (no date filter)
    # ------------------------------------------------------------------
    print("\n[1] Fetching member list ...")
    users = get_user_list()
    print(f"    → {len(users)} member(s) found")
    for u in users[:3]:  # Show at most 3 for brevity
        print(f"      {u['user_id']} | {u['name']} | {u['gender']} | {u['birth']}")

    # Use the first user returned for subsequent calls, or fall back to the
    # hard-coded sample ID if the list is empty.
    user_id = users[0]["user_id"] if users else SAMPLE_USER_ID

    # Small delay to respect the rate limit (10 req/s per IP)
    time.sleep(0.2)

    # ------------------------------------------------------------------
    # 2. Member detail
    # ------------------------------------------------------------------
    print(f"\n[2] Fetching info for user '{user_id}' ...")
    info = get_user_info(user_id)
    print(f"    → {info}")

    time.sleep(0.2)

    # ------------------------------------------------------------------
    # 3. Static analysis list
    # ------------------------------------------------------------------
    print(f"\n[3] Fetching Static analysis list for '{user_id}' ...")
    static_list = get_user_static_analysis_list(user_id)
    print(f"    → {len(static_list)} record(s)")
    for r in static_list[:3]:
        print(f"      index={r['index']}  time={r['measurement_time']}  ver={r['version']}")

    time.sleep(0.2)

    # ------------------------------------------------------------------
    # 4. OHS analysis list
    # ------------------------------------------------------------------
    print(f"\n[4] Fetching OHS analysis list for '{user_id}' ...")
    ohs_list = get_user_ohs_analysis_list(user_id)
    print(f"    → {len(ohs_list)} record(s)")

    time.sleep(0.2)

    # ------------------------------------------------------------------
    # 5. OLS analysis list
    # ------------------------------------------------------------------
    print(f"\n[5] Fetching OLS analysis list for '{user_id}' ...")
    ols_list = get_user_ols_analysis_list(user_id)
    print(f"    → {len(ols_list)} record(s)")

    time.sleep(0.2)

    # ------------------------------------------------------------------
    # 6. Static analysis report (uses the first available record)
    # ------------------------------------------------------------------
    if static_list:
        first_static_index = static_list[0]["index"]
        print(f"\n[6] Fetching Static report (analysis_index={first_static_index}) ...")
        report = get_user_static_analysis_report(user_id, first_static_index)
        expiry = report.get("url_expiration_seconds", 86400)
        print(f"    → URLs valid for {expiry // 3600} hours")
        # Print the first URL of the first available report type
        for report_type, pages in report["reports"].items():
            if pages:
                print(f"      [{report_type}] page 0 → {pages[0]['presigned_url'][:80]}...")
                break
    else:
        print("\n[6] No Static analysis records — skipping report request")

    time.sleep(0.2)

    # ------------------------------------------------------------------
    # 7. OHS analysis report
    # ------------------------------------------------------------------
    if ohs_list:
        first_ohs_index = ohs_list[0]["index"]
        print(f"\n[7] Fetching OHS report (analysis_index={first_ohs_index}) ...")
        report = get_user_ohs_analysis_report(user_id, first_ohs_index)
        pages = report["reports"].get("ohs_result_sheet", [])
        print(f"    → {len(pages)} page(s) in ohs_result_sheet")
    else:
        print("\n[7] No OHS analysis records — skipping report request")

    time.sleep(0.2)

    # ------------------------------------------------------------------
    # 8. OLS analysis report
    # ------------------------------------------------------------------
    if ols_list:
        first_ols_index = ols_list[0]["index"]
        print(f"\n[8] Fetching OLS report (analysis_index={first_ols_index}) ...")
        report = get_user_ols_analysis_report(user_id, first_ols_index)
        pages = report["reports"].get("ols_result_sheet", [])
        print(f"    → {len(pages)} page(s) in ols_result_sheet")
    else:
        print("\n[8] No OLS analysis records — skipping report request")

    print("\nDone.")


if __name__ == "__main__":
    main()

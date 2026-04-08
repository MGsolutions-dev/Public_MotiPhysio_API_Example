/**
 * motiPhysioClient.js
 * ===================
 * Public Moti-Physio API — JavaScript (Node.js) Sample Client
 *
 * Demonstrates all 8 API endpoints using the `axios` library
 * with async/await for clean, readable asynchronous code.
 *
 * Requirements:
 *   npm install axios
 *
 * Usage:
 *   node motiPhysioClient.js
 */

const axios = require("axios"); // npm install axios

// ---------------------------------------------------------------------------
// Configuration — replace these values with your own credentials
// ---------------------------------------------------------------------------
const BASE_URL    = "https://api.motiphysio.com";
const PROGRAM_ID  = "YOUR_PROGRAM_ID";              // e.g. "1489"
const SECURITY_KEY = "YOUR_SECURITY_KEY";           // e.g. "b25klhxkp36v6fcx7qkz"

// Sample user ID used when the member list is empty
const SAMPLE_USER_ID = "1489-00001";

// ---------------------------------------------------------------------------
// Helper: small async delay to respect the rate limit (10 req/s per IP)
// ---------------------------------------------------------------------------
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// ---------------------------------------------------------------------------
// Helper: POST request wrapper
//
// Sends a POST request to `endpoint` with `payload` as the JSON body.
// Throws an Error when:
//   - The HTTP status is 4xx or 5xx (axios throws automatically)
//   - The response body contains an "error" key (business-logic error)
// ---------------------------------------------------------------------------
async function post(endpoint, payload) {
  const url = BASE_URL + endpoint;

  const response = await axios.post(url, payload, {
    headers: { "Content-Type": "application/json" },
    timeout: 30000, // 30 seconds
  });

  const data = response.data;

  // The API returns business-logic errors in the body with HTTP 200.
  // Always check for the "error" key before processing the response.
  if (data && typeof data === "object" && !Array.isArray(data) && "error" in data) {
    throw new Error(`API error: ${data.error}`);
  }

  return data;
}

// ---------------------------------------------------------------------------
// 1. get_user_list
//    Returns all members registered under your program.
//    startPeriod / endPeriod are optional Unix timestamps (seconds).
// ---------------------------------------------------------------------------
async function getUserList(startPeriod = null, endPeriod = null) {
  const payload = {
    program_id:   PROGRAM_ID,
    security_key: SECURITY_KEY,
  };

  // Only add optional fields when they are explicitly provided
  if (startPeriod !== null) payload.start_period = startPeriod;
  if (endPeriod   !== null) payload.end_period   = endPeriod;

  return post("/v1/get_user_list", payload);
}

// ---------------------------------------------------------------------------
// 2. get_user_info
//    Returns detailed profile information for a single member.
// ---------------------------------------------------------------------------
async function getUserInfo(userId) {
  const payload = {
    program_id:   PROGRAM_ID,
    security_key: SECURITY_KEY,
    user_id:      userId,
  };
  return post("/v1/get_user_info", payload);
}

// ---------------------------------------------------------------------------
// 3. get_user_static_analysis_list
//    Returns Static (posture) analysis records for a member.
//    The "index" in each record is the analysis_index for report endpoints.
// ---------------------------------------------------------------------------
async function getUserStaticAnalysisList(userId, startPeriod = null, endPeriod = null) {
  const payload = {
    program_id:   PROGRAM_ID,
    security_key: SECURITY_KEY,
    user_id:      userId,
  };
  if (startPeriod !== null) payload.start_period = startPeriod;
  if (endPeriod   !== null) payload.end_period   = endPeriod;

  return post("/v1/get_user_static_analysis_list", payload);
}

// ---------------------------------------------------------------------------
// 4. get_user_ohs_analysis_list
//    Returns OHS (Overhead Squat / core function) analysis records.
// ---------------------------------------------------------------------------
async function getUserOhsAnalysisList(userId, startPeriod = null, endPeriod = null) {
  const payload = {
    program_id:   PROGRAM_ID,
    security_key: SECURITY_KEY,
    user_id:      userId,
  };
  if (startPeriod !== null) payload.start_period = startPeriod;
  if (endPeriod   !== null) payload.end_period   = endPeriod;

  return post("/v1/get_user_ohs_analysis_list", payload);
}

// ---------------------------------------------------------------------------
// 5. get_user_ols_analysis_list
//    Returns OLS (One-Leg Stand / balance) analysis records.
// ---------------------------------------------------------------------------
async function getUserOlsAnalysisList(userId, startPeriod = null, endPeriod = null) {
  const payload = {
    program_id:   PROGRAM_ID,
    security_key: SECURITY_KEY,
    user_id:      userId,
  };
  if (startPeriod !== null) payload.start_period = startPeriod;
  if (endPeriod   !== null) payload.end_period   = endPeriod;

  return post("/v1/get_user_ols_analysis_list", payload);
}

// ---------------------------------------------------------------------------
// 6. get_user_static_analysis_report
//    Returns Presigned S3 URLs for Static analysis report images.
//    analysisIndex: 0-based index from getUserStaticAnalysisList().
//    URLs are valid for 24 hours.
// ---------------------------------------------------------------------------
async function getUserStaticAnalysisReport(userId, analysisIndex) {
  const payload = {
    program_id:     PROGRAM_ID,
    security_key:   SECURITY_KEY,
    user_id:        userId,
    analysis_index: analysisIndex,
  };
  return post("/v1/get_user_static_analysis_report", payload);
}

// ---------------------------------------------------------------------------
// 7. get_user_ohs_analysis_report
//    Returns Presigned S3 URLs for OHS report images.
//    analysisIndex: 0-based index from getUserOhsAnalysisList().
// ---------------------------------------------------------------------------
async function getUserOhsAnalysisReport(userId, analysisIndex) {
  const payload = {
    program_id:     PROGRAM_ID,
    security_key:   SECURITY_KEY,
    user_id:        userId,
    analysis_index: analysisIndex,
  };
  return post("/v1/get_user_ohs_analysis_report", payload);
}

// ---------------------------------------------------------------------------
// 8. get_user_ols_analysis_report
//    Returns Presigned S3 URLs for OLS report images (Left & Right).
//    analysisIndex: 0-based index from getUserOlsAnalysisList().
// ---------------------------------------------------------------------------
async function getUserOlsAnalysisReport(userId, analysisIndex) {
  const payload = {
    program_id:     PROGRAM_ID,
    security_key:   SECURITY_KEY,
    user_id:        userId,
    analysis_index: analysisIndex,
  };
  return post("/v1/get_user_ols_analysis_report", payload);
}

// ---------------------------------------------------------------------------
// Main — demonstrates all 8 endpoints in sequence
// ---------------------------------------------------------------------------
async function main() {
  console.log("=".repeat(60));
  console.log("  Public Moti-Physio API — JavaScript (Node.js) Sample");
  console.log("=".repeat(60));

  try {
    // ----------------------------------------------------------------
    // 1. Member list
    // ----------------------------------------------------------------
    console.log("\n[1] Fetching member list ...");
    const users = await getUserList();
    console.log(`    → ${users.length} member(s) found`);
    users.slice(0, 3).forEach((u) => {
      console.log(`      ${u.user_id} | ${u.name} | ${u.gender} | ${u.birth}`);
    });

    // Use the first user or fall back to the sample ID
    const userId = users.length > 0 ? users[0].user_id : SAMPLE_USER_ID;

    await sleep(200); // 200 ms between requests

    // ----------------------------------------------------------------
    // 2. Member detail
    // ----------------------------------------------------------------
    console.log(`\n[2] Fetching info for user '${userId}' ...`);
    const info = await getUserInfo(userId);
    console.log("    →", info);

    await sleep(200);

    // ----------------------------------------------------------------
    // 3. Static analysis list
    // ----------------------------------------------------------------
    console.log(`\n[3] Fetching Static analysis list for '${userId}' ...`);
    const staticList = await getUserStaticAnalysisList(userId);
    console.log(`    → ${staticList.length} record(s)`);
    staticList.slice(0, 3).forEach((r) => {
      console.log(`      index=${r.index}  time=${r.measurement_time}  ver=${r.version}`);
    });

    await sleep(200);

    // ----------------------------------------------------------------
    // 4. OHS analysis list
    // ----------------------------------------------------------------
    console.log(`\n[4] Fetching OHS analysis list for '${userId}' ...`);
    const ohsList = await getUserOhsAnalysisList(userId);
    console.log(`    → ${ohsList.length} record(s)`);

    await sleep(200);

    // ----------------------------------------------------------------
    // 5. OLS analysis list
    // ----------------------------------------------------------------
    console.log(`\n[5] Fetching OLS analysis list for '${userId}' ...`);
    const olsList = await getUserOlsAnalysisList(userId);
    console.log(`    → ${olsList.length} record(s)`);

    await sleep(200);

    // ----------------------------------------------------------------
    // 6. Static analysis report
    // ----------------------------------------------------------------
    if (staticList.length > 0) {
      const firstStaticIndex = staticList[0].index;
      console.log(`\n[6] Fetching Static report (analysis_index=${firstStaticIndex}) ...`);
      const report = await getUserStaticAnalysisReport(userId, firstStaticIndex);
      const expiryHours = (report.url_expiration_seconds || 86400) / 3600;
      console.log(`    → URLs valid for ${expiryHours} hours`);
      // Print the first URL from the first available report type
      for (const [reportType, pages] of Object.entries(report.reports)) {
        if (pages.length > 0) {
          console.log(`      [${reportType}] page 0 → ${pages[0].presigned_url.substring(0, 80)}...`);
          break;
        }
      }
    } else {
      console.log("\n[6] No Static analysis records — skipping report request");
    }

    await sleep(200);

    // ----------------------------------------------------------------
    // 7. OHS analysis report
    // ----------------------------------------------------------------
    if (ohsList.length > 0) {
      const firstOhsIndex = ohsList[0].index;
      console.log(`\n[7] Fetching OHS report (analysis_index=${firstOhsIndex}) ...`);
      const report = await getUserOhsAnalysisReport(userId, firstOhsIndex);
      const pages = report.reports.ohs_result_sheet || [];
      console.log(`    → ${pages.length} page(s) in ohs_result_sheet`);
    } else {
      console.log("\n[7] No OHS analysis records — skipping report request");
    }

    await sleep(200);

    // ----------------------------------------------------------------
    // 8. OLS analysis report
    // ----------------------------------------------------------------
    if (olsList.length > 0) {
      const firstOlsIndex = olsList[0].index;
      console.log(`\n[8] Fetching OLS report (analysis_index=${firstOlsIndex}) ...`);
      const report = await getUserOlsAnalysisReport(userId, firstOlsIndex);
      const pages = report.reports.ols_result_sheet || [];
      console.log(`    → ${pages.length} page(s) in ols_result_sheet`);
    } else {
      console.log("\n[8] No OLS analysis records — skipping report request");
    }

    console.log("\nDone.");
  } catch (err) {
    // Axios wraps HTTP errors; print the response body when available
    if (err.response) {
      console.error("HTTP error:", err.response.status, err.response.data);
    } else {
      console.error("Error:", err.message);
    }
    process.exit(1);
  }
}

main();

/**
 * MotiPhysioClient.java
 * =====================
 * Public Moti-Physio API — Java Sample Client
 *
 * Demonstrates all 8 API endpoints using the built-in java.net.http.HttpClient
 * (Java 11+) and the org.json library for JSON handling.
 *
 * Requirements:
 *   Java 11 or later
 *   org.json jar (https://mvnrepository.com/artifact/org.json/json)
 *     OR add to pom.xml / build.gradle — see README.md for details.
 *
 * Compile & run (with org.json on the classpath):
 *   javac -cp .:json-20231013.jar MotiPhysioClient.java
 *   java  -cp .:json-20231013.jar MotiPhysioClient
 */

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

public class MotiPhysioClient {

    // -----------------------------------------------------------------------
    // Configuration — replace these values with your own credentials
    // -----------------------------------------------------------------------
    private static final String BASE_URL     = "https://api.motiphysio.com";
    private static final String PROGRAM_ID   = "YOUR_PROGRAM_ID";
    private static final String SECURITY_KEY = "YOUR_SECURITY_KEY";

    // Shared HttpClient instance (thread-safe, reuse across requests)
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // -----------------------------------------------------------------------
    // Helper: POST request → returns raw JSON string
    //
    // Sends a POST request with the given JSON body.
    // Throws RuntimeException when:
    //   - The HTTP status is not 200
    //   - The response body contains an "error" key (business-logic error)
    // -----------------------------------------------------------------------
    private static String post(String endpoint, JSONObject payload) throws Exception {
        String url = BASE_URL + endpoint;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "HTTP error " + response.statusCode() + ": " + response.body());
        }

        String body = response.body();

        // Business-logic errors use HTTP 200 with {"error": "..."} in the body.
        // We detect them by checking whether the body starts with '{' and contains
        // an "error" key.
        if (body.startsWith("{")) {
            JSONObject obj = new JSONObject(body);
            if (obj.has("error")) {
                throw new RuntimeException("API error: " + obj.getString("error"));
            }
        }

        return body;
    }

    // -----------------------------------------------------------------------
    // Helper: sleep without checked exception (for readability in main)
    // -----------------------------------------------------------------------
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // -----------------------------------------------------------------------
    // 1. get_user_list
    //    Returns all members registered under your program.
    //    Pass 0 for startPeriod / endPeriod to omit the filter.
    // -----------------------------------------------------------------------
    public static JSONArray getUserList(long startPeriod, long endPeriod) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("program_id",   PROGRAM_ID);
        payload.put("security_key", SECURITY_KEY);

        // Only add optional date-filter fields when a non-zero value is given
        if (startPeriod > 0) payload.put("start_period", startPeriod);
        if (endPeriod   > 0) payload.put("end_period",   endPeriod);

        String body = post("/v1/get_user_list", payload);
        return new JSONArray(body);
    }

    // -----------------------------------------------------------------------
    // 2. get_user_info
    //    Returns detailed profile information for a single member.
    // -----------------------------------------------------------------------
    public static JSONObject getUserInfo(String userId) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("program_id",   PROGRAM_ID);
        payload.put("security_key", SECURITY_KEY);
        payload.put("user_id",      userId);

        String body = post("/v1/get_user_info", payload);
        return new JSONObject(body);
    }

    // -----------------------------------------------------------------------
    // 3. get_user_static_analysis_list
    //    Returns Static (posture) analysis records for a member.
    //    The "index" in each record → pass as analysis_index to report endpoint.
    // -----------------------------------------------------------------------
    public static JSONArray getUserStaticAnalysisList(
            String userId, long startPeriod, long endPeriod) throws Exception {

        JSONObject payload = new JSONObject();
        payload.put("program_id",   PROGRAM_ID);
        payload.put("security_key", SECURITY_KEY);
        payload.put("user_id",      userId);
        if (startPeriod > 0) payload.put("start_period", startPeriod);
        if (endPeriod   > 0) payload.put("end_period",   endPeriod);

        return new JSONArray(post("/v1/get_user_static_analysis_list", payload));
    }

    // -----------------------------------------------------------------------
    // 4. get_user_ohs_analysis_list
    //    Returns OHS (Overhead Squat / core function) analysis records.
    // -----------------------------------------------------------------------
    public static JSONArray getUserOhsAnalysisList(
            String userId, long startPeriod, long endPeriod) throws Exception {

        JSONObject payload = new JSONObject();
        payload.put("program_id",   PROGRAM_ID);
        payload.put("security_key", SECURITY_KEY);
        payload.put("user_id",      userId);
        if (startPeriod > 0) payload.put("start_period", startPeriod);
        if (endPeriod   > 0) payload.put("end_period",   endPeriod);

        return new JSONArray(post("/v1/get_user_ohs_analysis_list", payload));
    }

    // -----------------------------------------------------------------------
    // 5. get_user_ols_analysis_list
    //    Returns OLS (One-Leg Stand / balance) analysis records.
    // -----------------------------------------------------------------------
    public static JSONArray getUserOlsAnalysisList(
            String userId, long startPeriod, long endPeriod) throws Exception {

        JSONObject payload = new JSONObject();
        payload.put("program_id",   PROGRAM_ID);
        payload.put("security_key", SECURITY_KEY);
        payload.put("user_id",      userId);
        if (startPeriod > 0) payload.put("start_period", startPeriod);
        if (endPeriod   > 0) payload.put("end_period",   endPeriod);

        return new JSONArray(post("/v1/get_user_ols_analysis_list", payload));
    }

    // -----------------------------------------------------------------------
    // 6. get_user_static_analysis_report
    //    Returns Presigned S3 URLs for all Static analysis report images.
    //    analysisIndex: 0-based index from getUserStaticAnalysisList().
    //    URLs are valid for 24 hours (86400 seconds).
    // -----------------------------------------------------------------------
    public static JSONObject getUserStaticAnalysisReport(
            String userId, int analysisIndex) throws Exception {

        JSONObject payload = new JSONObject();
        payload.put("program_id",     PROGRAM_ID);
        payload.put("security_key",   SECURITY_KEY);
        payload.put("user_id",        userId);
        payload.put("analysis_index", analysisIndex);

        return new JSONObject(post("/v1/get_user_static_analysis_report", payload));
    }

    // -----------------------------------------------------------------------
    // 7. get_user_ohs_analysis_report
    //    Returns Presigned S3 URLs for OHS report images.
    //    analysisIndex: 0-based index from getUserOhsAnalysisList().
    // -----------------------------------------------------------------------
    public static JSONObject getUserOhsAnalysisReport(
            String userId, int analysisIndex) throws Exception {

        JSONObject payload = new JSONObject();
        payload.put("program_id",     PROGRAM_ID);
        payload.put("security_key",   SECURITY_KEY);
        payload.put("user_id",        userId);
        payload.put("analysis_index", analysisIndex);

        return new JSONObject(post("/v1/get_user_ohs_analysis_report", payload));
    }

    // -----------------------------------------------------------------------
    // 8. get_user_ols_analysis_report
    //    Returns Presigned S3 URLs for OLS report images (Left & Right).
    //    analysisIndex: 0-based index from getUserOlsAnalysisList().
    // -----------------------------------------------------------------------
    public static JSONObject getUserOlsAnalysisReport(
            String userId, int analysisIndex) throws Exception {

        JSONObject payload = new JSONObject();
        payload.put("program_id",     PROGRAM_ID);
        payload.put("security_key",   SECURITY_KEY);
        payload.put("user_id",        userId);
        payload.put("analysis_index", analysisIndex);

        return new JSONObject(post("/v1/get_user_ols_analysis_report", payload));
    }

    // -----------------------------------------------------------------------
    // main — demonstrates all 8 endpoints in sequence
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("  Public Moti-Physio API — Java Sample");
        System.out.println("=".repeat(60));

        try {
            // ------------------------------------------------------------
            // 1. Member list
            // ------------------------------------------------------------
            System.out.println("\n[1] Fetching member list ...");
            JSONArray users = getUserList(0, 0); // 0 = no filter
            System.out.printf("    → %d member(s) found%n", users.length());
            for (int i = 0; i < Math.min(3, users.length()); i++) {
                JSONObject u = users.getJSONObject(i);
                System.out.printf("      %s | %s | %s | %s%n",
                        u.getString("user_id"),
                        u.getString("name"),
                        u.getString("gender"),
                        u.getString("birth"));
            }

            // Use the first user or fall back to the sample ID
            String userId = users.length() > 0
                    ? users.getJSONObject(0).getString("user_id")
                    : "1489-00001";

            sleep(200);

            // ------------------------------------------------------------
            // 2. Member detail
            // ------------------------------------------------------------
            System.out.printf("%n[2] Fetching info for user '%s' ...%n", userId);
            JSONObject info = getUserInfo(userId);
            System.out.println("    → " + info.toString(2));

            sleep(200);

            // ------------------------------------------------------------
            // 3. Static analysis list
            // ------------------------------------------------------------
            System.out.printf("%n[3] Fetching Static analysis list for '%s' ...%n", userId);
            JSONArray staticList = getUserStaticAnalysisList(userId, 0, 0);
            System.out.printf("    → %d record(s)%n", staticList.length());
            for (int i = 0; i < Math.min(3, staticList.length()); i++) {
                JSONObject r = staticList.getJSONObject(i);
                System.out.printf("      index=%d  time=%d  ver=%s%n",
                        r.getInt("index"),
                        r.getLong("measurement_time"),
                        r.getString("version"));
            }

            sleep(200);

            // ------------------------------------------------------------
            // 4. OHS analysis list
            // ------------------------------------------------------------
            System.out.printf("%n[4] Fetching OHS analysis list for '%s' ...%n", userId);
            JSONArray ohsList = getUserOhsAnalysisList(userId, 0, 0);
            System.out.printf("    → %d record(s)%n", ohsList.length());

            sleep(200);

            // ------------------------------------------------------------
            // 5. OLS analysis list
            // ------------------------------------------------------------
            System.out.printf("%n[5] Fetching OLS analysis list for '%s' ...%n", userId);
            JSONArray olsList = getUserOlsAnalysisList(userId, 0, 0);
            System.out.printf("    → %d record(s)%n", olsList.length());

            sleep(200);

            // ------------------------------------------------------------
            // 6. Static analysis report
            // ------------------------------------------------------------
            if (staticList.length() > 0) {
                int firstStaticIndex = staticList.getJSONObject(0).getInt("index");
                System.out.printf("%n[6] Fetching Static report (analysis_index=%d) ...%n",
                        firstStaticIndex);
                JSONObject report = getUserStaticAnalysisReport(userId, firstStaticIndex);
                int expiryHours = report.optInt("url_expiration_seconds", 86400) / 3600;
                System.out.printf("    → URLs valid for %d hours%n", expiryHours);

                // Print the first URL from the first available report type
                JSONObject reports = report.getJSONObject("reports");
                for (String key : reports.keySet()) {
                    JSONArray pages = reports.getJSONArray(key);
                    if (pages.length() > 0) {
                        String url = pages.getJSONObject(0).getString("presigned_url");
                        System.out.printf("      [%s] page 0 → %s...%n",
                                key, url.substring(0, Math.min(80, url.length())));
                        break;
                    }
                }
            } else {
                System.out.println("\n[6] No Static analysis records — skipping report request");
            }

            sleep(200);

            // ------------------------------------------------------------
            // 7. OHS analysis report
            // ------------------------------------------------------------
            if (ohsList.length() > 0) {
                int firstOhsIndex = ohsList.getJSONObject(0).getInt("index");
                System.out.printf("%n[7] Fetching OHS report (analysis_index=%d) ...%n",
                        firstOhsIndex);
                JSONObject report = getUserOhsAnalysisReport(userId, firstOhsIndex);
                JSONArray pages = report.getJSONObject("reports")
                                        .optJSONArray("ohs_result_sheet");
                System.out.printf("    → %d page(s) in ohs_result_sheet%n",
                        pages != null ? pages.length() : 0);
            } else {
                System.out.println("\n[7] No OHS analysis records — skipping report request");
            }

            sleep(200);

            // ------------------------------------------------------------
            // 8. OLS analysis report
            // ------------------------------------------------------------
            if (olsList.length() > 0) {
                int firstOlsIndex = olsList.getJSONObject(0).getInt("index");
                System.out.printf("%n[8] Fetching OLS report (analysis_index=%d) ...%n",
                        firstOlsIndex);
                JSONObject report = getUserOlsAnalysisReport(userId, firstOlsIndex);
                JSONArray pages = report.getJSONObject("reports")
                                        .optJSONArray("ols_result_sheet");
                System.out.printf("    → %d page(s) in ols_result_sheet%n",
                        pages != null ? pages.length() : 0);
            } else {
                System.out.println("\n[8] No OLS analysis records — skipping report request");
            }

            System.out.println("\nDone.");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}

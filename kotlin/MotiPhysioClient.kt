/**
 * MotiPhysioClient.kt
 * ===================
 * Public Moti-Physio API — Kotlin Sample Client
 *
 * Demonstrates all 8 API endpoints using:
 *   - OkHttp  for HTTP requests
 *   - Gson    for JSON serialization / deserialization
 *   - Kotlin Coroutines (Dispatchers.IO) for non-blocking network calls
 *
 * This structure is Android-ready and works on the JVM as well.
 *
 * Dependencies (add to build.gradle or build.gradle.kts):
 *   implementation("com.squareup.okhttp3:okhttp:4.12.0")
 *   implementation("com.google.code.gson:gson:2.10.1")
 *   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
 *
 * Run (JVM):
 *   kotlinc MotiPhysioClient.kt -include-runtime -cp <okhttp+gson jars> -d moti.jar
 *   java -cp moti.jar:<okhttp+gson jars> MotiPhysioClientKt
 *
 * For Android: copy MotiPhysioClient class into your project and call from a
 * ViewModel / coroutine scope.
 */

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Configuration — replace these values with your own credentials
// ---------------------------------------------------------------------------
private const val BASE_URL     = "https://api.motiphysio.com"
private const val PROGRAM_ID   = "YOUR_PROGRAM_ID"               // e.g. "1489"
private const val SECURITY_KEY = "YOUR_SECURITY_KEY"             // e.g. "b25klhxkp36v6fcx7qkz"

private const val SAMPLE_USER_ID = "1489-00001"

// ---------------------------------------------------------------------------
// Data classes (Gson will map JSON fields to these)
// ---------------------------------------------------------------------------

/** Member summary — returned by getUserList */
data class UserSummary(
    val user_id:       String,
    val name:          String,
    val birth:         String,
    val gender:        String,
    val register_date: Long
)

/** Member detail — returned by getUserInfo */
data class UserDetail(
    val user_id:       String,
    val name:          String,
    val birth:         String,
    val gender:        String,
    val height:        Double?,
    val weight:        Double?,
    val register_date: Long
)

/** Analysis list item — shared by Static / OHS / OLS list endpoints */
data class AnalysisRecord(
    val index:            Int,
    val measurement_time: Long,
    val version:          String
)

/** A single report page entry with a Presigned URL */
data class ReportPage(
    val page_index:    Int,
    val filename:      String,
    val presigned_url: String
)

/** Static analysis report response */
data class StaticReportResponse(
    val user_id:                String,
    val analysis_index:         Int,
    val url_expiration_seconds: Int,
    val reports:                StaticReports
)
data class StaticReports(
    val skeleton_result_sheet:       List<ReportPage>,
    val expert_result_sheet:         List<ReportPage>,
    val original_image_result_sheet: List<ReportPage>,
    val original_image:              List<ReportPage>,
    val risk_ranking_result_sheet:   List<ReportPage>
)

/** OHS analysis report response */
data class OhsReportResponse(
    val user_id:                String,
    val analysis_index:         Int,
    val url_expiration_seconds: Int,
    val reports:                OhsReports
)
data class OhsReports(val ohs_result_sheet: List<ReportPage>)

/** OLS analysis report response */
data class OlsReportResponse(
    val user_id:                String,
    val analysis_index:         Int,
    val url_expiration_seconds: Int,
    val reports:                OlsReports
)
data class OlsReports(val ols_result_sheet: List<ReportPage>)

// ---------------------------------------------------------------------------
// Custom exception for API-level errors
// ---------------------------------------------------------------------------
class ApiException(message: String) : Exception(message)

// ---------------------------------------------------------------------------
// MotiPhysioClient
// ---------------------------------------------------------------------------
class MotiPhysioClient {

    private val gson = Gson()

    // Shared OkHttpClient with connect / read / write timeouts
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30,    TimeUnit.SECONDS)
        .writeTimeout(30,   TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // -----------------------------------------------------------------------
    // Helper: POST request → parsed response of type T
    //
    // Executes the network call on Dispatchers.IO (safe for coroutines/Android).
    // Throws ApiException when the response body contains {"error": "..."}.
    // -----------------------------------------------------------------------
    private suspend inline fun <reified T> post(
        endpoint: String,
        body: Map<String, Any?>
    ): T = withContext(Dispatchers.IO) {

        val jsonBody = gson.toJson(body).toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .post(jsonBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw ApiException("Empty response body")

        if (!response.isSuccessful) {
            throw ApiException("HTTP ${response.code}: $responseBody")
        }

        // Check for business-logic error payload (HTTP 200 with "error" key)
        val maybeError = gson.fromJson(responseBody, JsonObject::class.java)
        if (maybeError.has("error")) {
            throw ApiException("API error: ${maybeError.get("error").asString}")
        }

        // Deserialize to the requested type T
        gson.fromJson(responseBody, object : TypeToken<T>() {}.type)
    }

    // -----------------------------------------------------------------------
    // 1. getUserList
    //    Returns all members registered under your program.
    //    Pass null for startPeriod / endPeriod to omit date filtering.
    // -----------------------------------------------------------------------
    suspend fun getUserList(
        startPeriod: Long? = null,
        endPeriod:   Long? = null
    ): List<UserSummary> {
        val body = mutableMapOf<String, Any?>(
            "program_id"   to PROGRAM_ID,
            "security_key" to SECURITY_KEY
        )
        startPeriod?.let { body["start_period"] = it }
        endPeriod?.let   { body["end_period"]   = it }

        return post("/v1/get_user_list", body)
    }

    // -----------------------------------------------------------------------
    // 2. getUserInfo
    //    Returns detailed profile information for a single member.
    // -----------------------------------------------------------------------
    suspend fun getUserInfo(userId: String): UserDetail {
        val body = mapOf(
            "program_id"   to PROGRAM_ID,
            "security_key" to SECURITY_KEY,
            "user_id"      to userId
        )
        return post("/v1/get_user_info", body)
    }

    // -----------------------------------------------------------------------
    // 3. getUserStaticAnalysisList
    //    Returns Static (posture) analysis records.
    //    Use each record's "index" as analysis_index in getUserStaticAnalysisReport.
    // -----------------------------------------------------------------------
    suspend fun getUserStaticAnalysisList(
        userId:      String,
        startPeriod: Long? = null,
        endPeriod:   Long? = null
    ): List<AnalysisRecord> {
        val body = mutableMapOf<String, Any?>(
            "program_id"   to PROGRAM_ID,
            "security_key" to SECURITY_KEY,
            "user_id"      to userId
        )
        startPeriod?.let { body["start_period"] = it }
        endPeriod?.let   { body["end_period"]   = it }

        return post("/v1/get_user_static_analysis_list", body)
    }

    // -----------------------------------------------------------------------
    // 4. getUserOhsAnalysisList
    //    Returns OHS (Overhead Squat / core function) analysis records.
    // -----------------------------------------------------------------------
    suspend fun getUserOhsAnalysisList(
        userId:      String,
        startPeriod: Long? = null,
        endPeriod:   Long? = null
    ): List<AnalysisRecord> {
        val body = mutableMapOf<String, Any?>(
            "program_id"   to PROGRAM_ID,
            "security_key" to SECURITY_KEY,
            "user_id"      to userId
        )
        startPeriod?.let { body["start_period"] = it }
        endPeriod?.let   { body["end_period"]   = it }

        return post("/v1/get_user_ohs_analysis_list", body)
    }

    // -----------------------------------------------------------------------
    // 5. getUserOlsAnalysisList
    //    Returns OLS (One-Leg Stand / balance) analysis records.
    // -----------------------------------------------------------------------
    suspend fun getUserOlsAnalysisList(
        userId:      String,
        startPeriod: Long? = null,
        endPeriod:   Long? = null
    ): List<AnalysisRecord> {
        val body = mutableMapOf<String, Any?>(
            "program_id"   to PROGRAM_ID,
            "security_key" to SECURITY_KEY,
            "user_id"      to userId
        )
        startPeriod?.let { body["start_period"] = it }
        endPeriod?.let   { body["end_period"]   = it }

        return post("/v1/get_user_ols_analysis_list", body)
    }

    // -----------------------------------------------------------------------
    // 6. getUserStaticAnalysisReport
    //    Returns Presigned S3 URLs for Static analysis report images.
    //    analysisIndex: 0-based index from getUserStaticAnalysisList().
    //    URLs are valid for 24 hours (86400 seconds).
    // -----------------------------------------------------------------------
    suspend fun getUserStaticAnalysisReport(
        userId:        String,
        analysisIndex: Int
    ): StaticReportResponse {
        val body = mapOf(
            "program_id"     to PROGRAM_ID,
            "security_key"   to SECURITY_KEY,
            "user_id"        to userId,
            "analysis_index" to analysisIndex
        )
        return post("/v1/get_user_static_analysis_report", body)
    }

    // -----------------------------------------------------------------------
    // 7. getUserOhsAnalysisReport
    //    Returns Presigned S3 URLs for OHS report images.
    //    analysisIndex: 0-based index from getUserOhsAnalysisList().
    // -----------------------------------------------------------------------
    suspend fun getUserOhsAnalysisReport(
        userId:        String,
        analysisIndex: Int
    ): OhsReportResponse {
        val body = mapOf(
            "program_id"     to PROGRAM_ID,
            "security_key"   to SECURITY_KEY,
            "user_id"        to userId,
            "analysis_index" to analysisIndex
        )
        return post("/v1/get_user_ohs_analysis_report", body)
    }

    // -----------------------------------------------------------------------
    // 8. getUserOlsAnalysisReport
    //    Returns Presigned S3 URLs for OLS report images (Left & Right).
    //    analysisIndex: 0-based index from getUserOlsAnalysisList().
    // -----------------------------------------------------------------------
    suspend fun getUserOlsAnalysisReport(
        userId:        String,
        analysisIndex: Int
    ): OlsReportResponse {
        val body = mapOf(
            "program_id"     to PROGRAM_ID,
            "security_key"   to SECURITY_KEY,
            "user_id"        to userId,
            "analysis_index" to analysisIndex
        )
        return post("/v1/get_user_ols_analysis_report", body)
    }
}

// ---------------------------------------------------------------------------
// main — demonstrates all 8 endpoints in sequence
// runBlocking is used here for the top-level entry point only.
// In Android, use viewModelScope.launch { } instead.
// ---------------------------------------------------------------------------
fun main() = runBlocking {
    println("=".repeat(60))
    println("  Public Moti-Physio API — Kotlin Sample")
    println("=".repeat(60))

    val client = MotiPhysioClient()

    try {
        // ----------------------------------------------------------------
        // 1. Member list
        // ----------------------------------------------------------------
        println("\n[1] Fetching member list ...")
        val users = client.getUserList()
        println("    → ${users.size} member(s) found")
        users.take(3).forEach { u ->
            println("      ${u.user_id} | ${u.name} | ${u.gender} | ${u.birth}")
        }

        val userId = users.firstOrNull()?.user_id ?: SAMPLE_USER_ID

        delay(200) // 200 ms — stay within 10 req/s rate limit

        // ----------------------------------------------------------------
        // 2. Member detail
        // ----------------------------------------------------------------
        println("\n[2] Fetching info for user '$userId' ...")
        val info = client.getUserInfo(userId)
        println("    → name: ${info.name}, height: ${info.height} cm, weight: ${info.weight} kg")

        delay(200)

        // ----------------------------------------------------------------
        // 3. Static analysis list
        // ----------------------------------------------------------------
        println("\n[3] Fetching Static analysis list for '$userId' ...")
        val staticList = client.getUserStaticAnalysisList(userId)
        println("    → ${staticList.size} record(s)")
        staticList.take(3).forEach { r ->
            println("      index=${r.index}  time=${r.measurement_time}  ver=${r.version}")
        }

        delay(200)

        // ----------------------------------------------------------------
        // 4. OHS analysis list
        // ----------------------------------------------------------------
        println("\n[4] Fetching OHS analysis list for '$userId' ...")
        val ohsList = client.getUserOhsAnalysisList(userId)
        println("    → ${ohsList.size} record(s)")

        delay(200)

        // ----------------------------------------------------------------
        // 5. OLS analysis list
        // ----------------------------------------------------------------
        println("\n[5] Fetching OLS analysis list for '$userId' ...")
        val olsList = client.getUserOlsAnalysisList(userId)
        println("    → ${olsList.size} record(s)")

        delay(200)

        // ----------------------------------------------------------------
        // 6. Static analysis report
        // ----------------------------------------------------------------
        val firstStatic = staticList.firstOrNull()
        if (firstStatic != null) {
            println("\n[6] Fetching Static report (analysis_index=${firstStatic.index}) ...")
            val report = client.getUserStaticAnalysisReport(userId, firstStatic.index)
            val hours = report.url_expiration_seconds / 3600
            println("    → URLs valid for $hours hours")
            report.reports.skeleton_result_sheet.firstOrNull()?.let { page ->
                println("      [skeleton_result_sheet] page 0 → ${page.presigned_url.take(80)}...")
            }
        } else {
            println("\n[6] No Static analysis records — skipping report request")
        }

        delay(200)

        // ----------------------------------------------------------------
        // 7. OHS analysis report
        // ----------------------------------------------------------------
        val firstOhs = ohsList.firstOrNull()
        if (firstOhs != null) {
            println("\n[7] Fetching OHS report (analysis_index=${firstOhs.index}) ...")
            val report = client.getUserOhsAnalysisReport(userId, firstOhs.index)
            println("    → ${report.reports.ohs_result_sheet.size} page(s) in ohs_result_sheet")
        } else {
            println("\n[7] No OHS analysis records — skipping report request")
        }

        delay(200)

        // ----------------------------------------------------------------
        // 8. OLS analysis report
        // ----------------------------------------------------------------
        val firstOls = olsList.firstOrNull()
        if (firstOls != null) {
            println("\n[8] Fetching OLS report (analysis_index=${firstOls.index}) ...")
            val report = client.getUserOlsAnalysisReport(userId, firstOls.index)
            println("    → ${report.reports.ols_result_sheet.size} page(s) in ols_result_sheet")
        } else {
            println("\n[8] No OLS analysis records — skipping report request")
        }

        println("\nDone.")

    } catch (e: ApiException) {
        System.err.println("API error: ${e.message}")
    } catch (e: Exception) {
        System.err.println("Unexpected error: ${e.message}")
    }
}

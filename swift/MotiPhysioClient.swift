/**
 * MotiPhysioClient.swift
 * ======================
 * Public Moti-Physio API — Swift Sample Client
 *
 * Demonstrates all 8 API endpoints using URLSession with
 * async/await (Swift 5.5+, iOS 15+, macOS 12+).
 *
 * No external dependencies — uses only the Swift standard library
 * and Foundation.
 *
 * Usage (macOS command-line):
 *   swiftc MotiPhysioClient.swift -o motiPhysio
 *   ./motiPhysio
 *
 * For iOS/macOS apps: copy the MotiPhysioClient class into your Xcode project.
 */

import Foundation

// ---------------------------------------------------------------------------
// Configuration — replace these values with your own credentials
// ---------------------------------------------------------------------------
let BASE_URL:     String = "https://api.motiphysio.com"
let PROGRAM_ID:   String = "YOUR_PROGRAM_ID"               // e.g. "1489"
let SECURITY_KEY: String = "YOUR_SECURITY_KEY"             // e.g. "b25klhxkp36v6fcx7qkz"

// Sample user used as a fallback when the member list is empty
let SAMPLE_USER_ID: String = "1489-00001"

// ---------------------------------------------------------------------------
// MotiPhysioClient
// A lightweight async client for the Public Moti-Physio API.
// ---------------------------------------------------------------------------
class MotiPhysioClient {

    // Shared URLSession with a 30-second timeout for all requests
    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        return URLSession(configuration: config)
    }()

    // -----------------------------------------------------------------------
    // Helper: POST request → decoded JSON
    //
    // Sends a POST request to `endpoint` with `body` as the JSON payload.
    // Returns the decoded response as the generic type T.
    //
    // Throws:
    //   - URLError on network failures
    //   - APIError.httpError on non-200 HTTP status
    //   - APIError.apiError when the response body contains an "error" key
    //   - DecodingError on JSON decoding failures
    // -----------------------------------------------------------------------
    private func post<T: Decodable>(
        endpoint: String,
        body: [String: Any]
    ) async throws -> T {

        guard let url = URL(string: BASE_URL + endpoint) else {
            throw URLError(.badURL)
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await session.data(for: request)

        // Check HTTP status code
        if let httpResponse = response as? HTTPURLResponse,
           httpResponse.statusCode != 200 {
            throw APIError.httpError(httpResponse.statusCode)
        }

        // Check for business-logic error in the body ({"error": "..."})
        // The API returns HTTP 200 even for auth/data errors.
        if let errorObj = try? JSONDecoder().decode(ErrorResponse.self, from: data),
           let message = errorObj.error {
            throw APIError.apiError(message)
        }

        return try JSONDecoder().decode(T.self, from: data)
    }

    // -----------------------------------------------------------------------
    // 1. getUserList
    //    Returns all members registered under your program.
    //    Pass nil for startPeriod / endPeriod to omit date filtering.
    // -----------------------------------------------------------------------
    func getUserList(
        startPeriod: Int? = nil,
        endPeriod:   Int? = nil
    ) async throws -> [UserSummary] {

        var body: [String: Any] = [
            "program_id":   PROGRAM_ID,
            "security_key": SECURITY_KEY,
        ]
        if let s = startPeriod { body["start_period"] = s }
        if let e = endPeriod   { body["end_period"]   = e }

        return try await post(endpoint: "/v1/get_user_list", body: body)
    }

    // -----------------------------------------------------------------------
    // 2. getUserInfo
    //    Returns detailed profile information for a single member.
    // -----------------------------------------------------------------------
    func getUserInfo(userId: String) async throws -> UserDetail {
        let body: [String: Any] = [
            "program_id":   PROGRAM_ID,
            "security_key": SECURITY_KEY,
            "user_id":      userId,
        ]
        return try await post(endpoint: "/v1/get_user_info", body: body)
    }

    // -----------------------------------------------------------------------
    // 3. getUserStaticAnalysisList
    //    Returns Static (posture) analysis records.
    //    Use the "index" field as analysis_index in getUserStaticAnalysisReport.
    // -----------------------------------------------------------------------
    func getUserStaticAnalysisList(
        userId:      String,
        startPeriod: Int? = nil,
        endPeriod:   Int? = nil
    ) async throws -> [AnalysisRecord] {

        var body: [String: Any] = [
            "program_id":   PROGRAM_ID,
            "security_key": SECURITY_KEY,
            "user_id":      userId,
        ]
        if let s = startPeriod { body["start_period"] = s }
        if let e = endPeriod   { body["end_period"]   = e }

        return try await post(endpoint: "/v1/get_user_static_analysis_list", body: body)
    }

    // -----------------------------------------------------------------------
    // 4. getUserOhsAnalysisList
    //    Returns OHS (core function) analysis records.
    // -----------------------------------------------------------------------
    func getUserOhsAnalysisList(
        userId:      String,
        startPeriod: Int? = nil,
        endPeriod:   Int? = nil
    ) async throws -> [AnalysisRecord] {

        var body: [String: Any] = [
            "program_id":   PROGRAM_ID,
            "security_key": SECURITY_KEY,
            "user_id":      userId,
        ]
        if let s = startPeriod { body["start_period"] = s }
        if let e = endPeriod   { body["end_period"]   = e }

        return try await post(endpoint: "/v1/get_user_ohs_analysis_list", body: body)
    }

    // -----------------------------------------------------------------------
    // 5. getUserOlsAnalysisList
    //    Returns OLS (balance) analysis records.
    // -----------------------------------------------------------------------
    func getUserOlsAnalysisList(
        userId:      String,
        startPeriod: Int? = nil,
        endPeriod:   Int? = nil
    ) async throws -> [AnalysisRecord] {

        var body: [String: Any] = [
            "program_id":   PROGRAM_ID,
            "security_key": SECURITY_KEY,
            "user_id":      userId,
        ]
        if let s = startPeriod { body["start_period"] = s }
        if let e = endPeriod   { body["end_period"]   = e }

        return try await post(endpoint: "/v1/get_user_ols_analysis_list", body: body)
    }

    // -----------------------------------------------------------------------
    // 6. getUserStaticAnalysisReport
    //    Returns Presigned S3 URLs for Static analysis report images.
    //    analysisIndex: 0-based index from getUserStaticAnalysisList().
    //    URLs expire after 24 hours (url_expiration_seconds = 86400).
    // -----------------------------------------------------------------------
    func getUserStaticAnalysisReport(
        userId:        String,
        analysisIndex: Int
    ) async throws -> StaticReportResponse {

        let body: [String: Any] = [
            "program_id":     PROGRAM_ID,
            "security_key":   SECURITY_KEY,
            "user_id":        userId,
            "analysis_index": analysisIndex,
        ]
        return try await post(endpoint: "/v1/get_user_static_analysis_report", body: body)
    }

    // -----------------------------------------------------------------------
    // 7. getUserOhsAnalysisReport
    //    Returns Presigned S3 URLs for OHS report images.
    //    analysisIndex: 0-based index from getUserOhsAnalysisList().
    // -----------------------------------------------------------------------
    func getUserOhsAnalysisReport(
        userId:        String,
        analysisIndex: Int
    ) async throws -> OhsReportResponse {

        let body: [String: Any] = [
            "program_id":     PROGRAM_ID,
            "security_key":   SECURITY_KEY,
            "user_id":        userId,
            "analysis_index": analysisIndex,
        ]
        return try await post(endpoint: "/v1/get_user_ohs_analysis_report", body: body)
    }

    // -----------------------------------------------------------------------
    // 8. getUserOlsAnalysisReport
    //    Returns Presigned S3 URLs for OLS report images (Left & Right).
    //    analysisIndex: 0-based index from getUserOlsAnalysisList().
    // -----------------------------------------------------------------------
    func getUserOlsAnalysisReport(
        userId:        String,
        analysisIndex: Int
    ) async throws -> OlsReportResponse {

        let body: [String: Any] = [
            "program_id":     PROGRAM_ID,
            "security_key":   SECURITY_KEY,
            "user_id":        userId,
            "analysis_index": analysisIndex,
        ]
        return try await post(endpoint: "/v1/get_user_ols_analysis_report", body: body)
    }
}

// ---------------------------------------------------------------------------
// Error types
// ---------------------------------------------------------------------------
enum APIError: Error, LocalizedError {
    case httpError(Int)
    case apiError(String)

    var errorDescription: String? {
        switch self {
        case .httpError(let code):  return "HTTP error \(code)"
        case .apiError(let msg):    return "API error: \(msg)"
        }
    }
}

// ---------------------------------------------------------------------------
// Response model types (Codable)
// ---------------------------------------------------------------------------

/// Used only to detect {"error": "..."} in the response body
struct ErrorResponse: Decodable {
    let error: String?
}

/// Member summary (returned by get_user_list)
struct UserSummary: Decodable {
    let user_id:       String
    let name:          String
    let birth:         String
    let gender:        String
    let register_date: Int
}

/// Member detail (returned by get_user_info)
struct UserDetail: Decodable {
    let user_id:       String
    let name:          String
    let birth:         String
    let gender:        String
    let height:        Double?
    let weight:        Double?
    let register_date: Int
}

/// Analysis list item (shared by Static / OHS / OLS list endpoints)
struct AnalysisRecord: Decodable {
    let index:            Int
    let measurement_time: Int
    let version:          String
}

/// A single report page with a Presigned URL
struct ReportPage: Decodable {
    let page_index:    Int
    let filename:      String
    let presigned_url: String
}

// --- Static report response ---
struct StaticReportResponse: Decodable {
    let user_id:                  String
    let analysis_index:           Int
    let url_expiration_seconds:   Int
    let reports:                  StaticReports
}
struct StaticReports: Decodable {
    let skeleton_result_sheet:        [ReportPage]
    let expert_result_sheet:          [ReportPage]
    let original_image_result_sheet:  [ReportPage]
    let original_image:               [ReportPage]
    let risk_ranking_result_sheet:    [ReportPage]
}

// --- OHS report response ---
struct OhsReportResponse: Decodable {
    let user_id:                String
    let analysis_index:         Int
    let url_expiration_seconds: Int
    let reports:                OhsReports
}
struct OhsReports: Decodable {
    let ohs_result_sheet: [ReportPage]
}

// --- OLS report response ---
struct OlsReportResponse: Decodable {
    let user_id:                String
    let analysis_index:         Int
    let url_expiration_seconds: Int
    let reports:                OlsReports
}
struct OlsReports: Decodable {
    let ols_result_sheet: [ReportPage]
}

// ---------------------------------------------------------------------------
// Main entry point — demonstrates all 8 endpoints
// ---------------------------------------------------------------------------
@main
struct Main {
    static func main() async {
        print(String(repeating: "=", count: 60))
        print("  Public Moti-Physio API — Swift Sample")
        print(String(repeating: "=", count: 60))

        let client = MotiPhysioClient()

        do {
            // ----------------------------------------------------------------
            // 1. Member list
            // ----------------------------------------------------------------
            print("\n[1] Fetching member list ...")
            let users = try await client.getUserList()
            print("    → \(users.count) member(s) found")
            for u in users.prefix(3) {
                print("      \(u.user_id) | \(u.name) | \(u.gender) | \(u.birth)")
            }

            let userId = users.first?.user_id ?? SAMPLE_USER_ID

            try await Task.sleep(nanoseconds: 200_000_000) // 200 ms

            // ----------------------------------------------------------------
            // 2. Member detail
            // ----------------------------------------------------------------
            print("\n[2] Fetching info for user '\(userId)' ...")
            let info = try await client.getUserInfo(userId: userId)
            print("    → name: \(info.name), height: \(info.height ?? 0) cm, weight: \(info.weight ?? 0) kg")

            try await Task.sleep(nanoseconds: 200_000_000)

            // ----------------------------------------------------------------
            // 3. Static analysis list
            // ----------------------------------------------------------------
            print("\n[3] Fetching Static analysis list for '\(userId)' ...")
            let staticList = try await client.getUserStaticAnalysisList(userId: userId)
            print("    → \(staticList.count) record(s)")
            for r in staticList.prefix(3) {
                print("      index=\(r.index)  time=\(r.measurement_time)  ver=\(r.version)")
            }

            try await Task.sleep(nanoseconds: 200_000_000)

            // ----------------------------------------------------------------
            // 4. OHS analysis list
            // ----------------------------------------------------------------
            print("\n[4] Fetching OHS analysis list for '\(userId)' ...")
            let ohsList = try await client.getUserOhsAnalysisList(userId: userId)
            print("    → \(ohsList.count) record(s)")

            try await Task.sleep(nanoseconds: 200_000_000)

            // ----------------------------------------------------------------
            // 5. OLS analysis list
            // ----------------------------------------------------------------
            print("\n[5] Fetching OLS analysis list for '\(userId)' ...")
            let olsList = try await client.getUserOlsAnalysisList(userId: userId)
            print("    → \(olsList.count) record(s)")

            try await Task.sleep(nanoseconds: 200_000_000)

            // ----------------------------------------------------------------
            // 6. Static analysis report
            // ----------------------------------------------------------------
            if let firstStatic = staticList.first {
                print("\n[6] Fetching Static report (analysis_index=\(firstStatic.index)) ...")
                let report = try await client.getUserStaticAnalysisReport(
                    userId: userId, analysisIndex: firstStatic.index)
                let hours = report.url_expiration_seconds / 3600
                print("    → URLs valid for \(hours) hours")
                if let page = report.reports.skeleton_result_sheet.first {
                    let preview = String(page.presigned_url.prefix(80))
                    print("      [skeleton_result_sheet] page 0 → \(preview)...")
                }
            } else {
                print("\n[6] No Static analysis records — skipping report request")
            }

            try await Task.sleep(nanoseconds: 200_000_000)

            // ----------------------------------------------------------------
            // 7. OHS analysis report
            // ----------------------------------------------------------------
            if let firstOhs = ohsList.first {
                print("\n[7] Fetching OHS report (analysis_index=\(firstOhs.index)) ...")
                let report = try await client.getUserOhsAnalysisReport(
                    userId: userId, analysisIndex: firstOhs.index)
                print("    → \(report.reports.ohs_result_sheet.count) page(s) in ohs_result_sheet")
            } else {
                print("\n[7] No OHS analysis records — skipping report request")
            }

            try await Task.sleep(nanoseconds: 200_000_000)

            // ----------------------------------------------------------------
            // 8. OLS analysis report
            // ----------------------------------------------------------------
            if let firstOls = olsList.first {
                print("\n[8] Fetching OLS report (analysis_index=\(firstOls.index)) ...")
                let report = try await client.getUserOlsAnalysisReport(
                    userId: userId, analysisIndex: firstOls.index)
                print("    → \(report.reports.ols_result_sheet.count) page(s) in ols_result_sheet")
            } else {
                print("\n[8] No OLS analysis records — skipping report request")
            }

            print("\nDone.")

        } catch let error as APIError {
            print("API error: \(error.localizedDescription)")
        } catch {
            print("Unexpected error: \(error)")
        }
    }
}

/**
 * MotiPhysioClient.cs
 * ===================
 * Public Moti-Physio API — C# Sample Client
 *
 * 모든 8개 엔드포인트를 .NET 내장 HttpClient 와
 * System.Text.Json 을 사용해 호출하는 예제입니다.
 *
 * ── 요구 사항 ───────────────────────────────────────────────────────────────
 *   .NET 6.0 이상 (HttpClient, async/await, System.Text.Json 내장 지원)
 *   외부 패키지 불필요
 *
 * ── 실행 방법 ───────────────────────────────────────────────────────────────
 *   dotnet run   (csharp/ 폴더에서)
 *
 * ── ASP.NET Core / Blazor / MAUI 프로젝트에 통합 ───────────────────────────
 *   MotiPhysioClient 클래스를 복사하고 DI 컨테이너에 등록하세요:
 *     builder.Services.AddHttpClient<MotiPhysioClient>();
 */

using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

// ─────────────────────────────────────────────────────────────────────────────
// 설정값 — 실제 발급받은 값으로 교체하세요
// ─────────────────────────────────────────────────────────────────────────────
static class Config
{
    public const string BaseUrl     = "https://api.motiphysio.com";
    public const string ProgramId   = "YOUR_PROGRAM_ID";               // 예: "1489"
    public const string SecurityKey = "YOUR_SECURITY_KEY";             // 예: "b25klhxkp36v6fcx7qkz"
    public const string SampleUserId = "1489-00001";
}

// ─────────────────────────────────────────────────────────────────────────────
// 응답 모델 (System.Text.Json 의 JsonPropertyName 으로 snake_case 매핑)
// ─────────────────────────────────────────────────────────────────────────────

/// <summary>get_user_list 응답 항목</summary>
record UserSummary(
    [property: JsonPropertyName("user_id")]       string UserId,
    [property: JsonPropertyName("name")]          string Name,
    [property: JsonPropertyName("birth")]         string Birth,
    [property: JsonPropertyName("gender")]        string Gender,
    [property: JsonPropertyName("register_date")] long   RegisterDate
);

/// <summary>get_user_info 응답</summary>
record UserDetail(
    [property: JsonPropertyName("user_id")]       string  UserId,
    [property: JsonPropertyName("name")]          string  Name,
    [property: JsonPropertyName("birth")]         string  Birth,
    [property: JsonPropertyName("gender")]        string  Gender,
    [property: JsonPropertyName("height")]        double? Height,
    [property: JsonPropertyName("weight")]        double? Weight,
    [property: JsonPropertyName("register_date")] long    RegisterDate
);

/// <summary>분석 목록 공통 항목 (Static / OHS / OLS 공유)</summary>
record AnalysisRecord(
    [property: JsonPropertyName("index")]            int    Index,
    [property: JsonPropertyName("measurement_time")] long   MeasurementTime,
    [property: JsonPropertyName("version")]          string Version
);

/// <summary>리포트 페이지 항목 (Presigned URL 포함)</summary>
record ReportPage(
    [property: JsonPropertyName("page_index")]    int    PageIndex,
    [property: JsonPropertyName("filename")]      string Filename,
    [property: JsonPropertyName("presigned_url")] string PresignedUrl
);

/// <summary>정적 분석 리포트 응답</summary>
record StaticReportResponse(
    [property: JsonPropertyName("user_id")]                 string       UserId,
    [property: JsonPropertyName("analysis_index")]          int          AnalysisIndex,
    [property: JsonPropertyName("url_expiration_seconds")]  int          UrlExpirationSeconds,
    [property: JsonPropertyName("reports")]                 StaticReports Reports
);
record StaticReports(
    [property: JsonPropertyName("skeleton_result_sheet")]        List<ReportPage> SkeletonResultSheet,
    [property: JsonPropertyName("expert_result_sheet")]          List<ReportPage> ExpertResultSheet,
    [property: JsonPropertyName("original_image_result_sheet")]  List<ReportPage> OriginalImageResultSheet,
    [property: JsonPropertyName("original_image")]               List<ReportPage> OriginalImage,
    [property: JsonPropertyName("risk_ranking_result_sheet")]    List<ReportPage> RiskRankingResultSheet
);

/// <summary>OHS 분석 리포트 응답</summary>
record OhsReportResponse(
    [property: JsonPropertyName("user_id")]                string    UserId,
    [property: JsonPropertyName("analysis_index")]         int       AnalysisIndex,
    [property: JsonPropertyName("url_expiration_seconds")] int       UrlExpirationSeconds,
    [property: JsonPropertyName("reports")]                OhsReports Reports
);
record OhsReports(
    [property: JsonPropertyName("ohs_result_sheet")] List<ReportPage> OhsResultSheet
);

/// <summary>OLS 분석 리포트 응답</summary>
record OlsReportResponse(
    [property: JsonPropertyName("user_id")]                string    UserId,
    [property: JsonPropertyName("analysis_index")]         int       AnalysisIndex,
    [property: JsonPropertyName("url_expiration_seconds")] int       UrlExpirationSeconds,
    [property: JsonPropertyName("reports")]                OlsReports Reports
);
record OlsReports(
    [property: JsonPropertyName("ols_result_sheet")] List<ReportPage> OlsResultSheet
);

// ─────────────────────────────────────────────────────────────────────────────
// 커스텀 예외
// ─────────────────────────────────────────────────────────────────────────────
class ApiException : Exception
{
    public ApiException(string message) : base(message) { }
}

// ─────────────────────────────────────────────────────────────────────────────
// MotiPhysioClient
// HttpClient 를 주입받아 사용하므로 DI 컨테이너와 함께 쓸 수 있습니다.
// ─────────────────────────────────────────────────────────────────────────────
class MotiPhysioClient
{
    private readonly HttpClient _http;

    // JSON 직렬화 옵션: null 값 필드는 출력 생략
    private static readonly JsonSerializerOptions _jsonOptions = new()
    {
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
    };

    public MotiPhysioClient(HttpClient http)
    {
        _http = http;
        _http.BaseAddress = new Uri(Config.BaseUrl);
        _http.Timeout     = TimeSpan.FromSeconds(30);
    }

    // -----------------------------------------------------------------------
    // 내부 헬퍼: POST → T 로 역직렬화
    //
    // 예외:
    //   ApiException — 응답 body 에 "error" 키가 있을 때
    //   HttpRequestException — HTTP 수준 오류
    // -----------------------------------------------------------------------
    private async Task<T> PostAsync<T>(string endpoint, object payload)
    {
        // 요청 전송 (Content-Type: application/json 자동 설정)
        HttpResponseMessage response = await _http.PostAsJsonAsync(endpoint, payload, _jsonOptions);
        response.EnsureSuccessStatusCode();

        string body = await response.Content.ReadAsStringAsync();

        // API 업무 오류 감지: HTTP 200 이지만 body 에 "error" 키가 있는 경우
        using JsonDocument doc = JsonDocument.Parse(body);
        if (doc.RootElement.TryGetProperty("error", out JsonElement errorElem))
        {
            throw new ApiException($"API error: {errorElem.GetString()}");
        }

        // 정상 응답 역직렬화
        return JsonSerializer.Deserialize<T>(body, _jsonOptions)
               ?? throw new ApiException("Null response body");
    }

    // -----------------------------------------------------------------------
    // 1. GetUserListAsync
    //    프로그램에 등록된 전체 회원 목록을 반환합니다.
    //    startPeriod / endPeriod : Unix timestamp (선택, null 이면 생략)
    // -----------------------------------------------------------------------
    public Task<List<UserSummary>> GetUserListAsync(
        long? startPeriod = null,
        long? endPeriod   = null)
    {
        var payload = new
        {
            program_id   = Config.ProgramId,
            security_key = Config.SecurityKey,
            start_period = startPeriod,  // null 이면 직렬화 생략
            end_period   = endPeriod
        };
        return PostAsync<List<UserSummary>>("/v1/get_user_list", payload);
    }

    // -----------------------------------------------------------------------
    // 2. GetUserInfoAsync
    //    특정 회원의 상세 프로필을 반환합니다.
    // -----------------------------------------------------------------------
    public Task<UserDetail> GetUserInfoAsync(string userId)
    {
        var payload = new
        {
            program_id   = Config.ProgramId,
            security_key = Config.SecurityKey,
            user_id      = userId
        };
        return PostAsync<UserDetail>("/v1/get_user_info", payload);
    }

    // -----------------------------------------------------------------------
    // 3. GetUserStaticAnalysisListAsync
    //    정적(자세) 분석 기록 목록을 반환합니다.
    //    각 항목의 Index → 리포트 요청 시 analysisIndex 로 사용합니다.
    // -----------------------------------------------------------------------
    public Task<List<AnalysisRecord>> GetUserStaticAnalysisListAsync(
        string userId,
        long?  startPeriod = null,
        long?  endPeriod   = null)
    {
        var payload = new
        {
            program_id   = Config.ProgramId,
            security_key = Config.SecurityKey,
            user_id      = userId,
            start_period = startPeriod,
            end_period   = endPeriod
        };
        return PostAsync<List<AnalysisRecord>>("/v1/get_user_static_analysis_list", payload);
    }

    // -----------------------------------------------------------------------
    // 4. GetUserOhsAnalysisListAsync
    //    OHS(핵심 기능/스쿼트) 분석 기록 목록을 반환합니다.
    // -----------------------------------------------------------------------
    public Task<List<AnalysisRecord>> GetUserOhsAnalysisListAsync(
        string userId,
        long?  startPeriod = null,
        long?  endPeriod   = null)
    {
        var payload = new
        {
            program_id   = Config.ProgramId,
            security_key = Config.SecurityKey,
            user_id      = userId,
            start_period = startPeriod,
            end_period   = endPeriod
        };
        return PostAsync<List<AnalysisRecord>>("/v1/get_user_ohs_analysis_list", payload);
    }

    // -----------------------------------------------------------------------
    // 5. GetUserOlsAnalysisListAsync
    //    OLS(균형/한발서기) 분석 기록 목록을 반환합니다.
    // -----------------------------------------------------------------------
    public Task<List<AnalysisRecord>> GetUserOlsAnalysisListAsync(
        string userId,
        long?  startPeriod = null,
        long?  endPeriod   = null)
    {
        var payload = new
        {
            program_id   = Config.ProgramId,
            security_key = Config.SecurityKey,
            user_id      = userId,
            start_period = startPeriod,
            end_period   = endPeriod
        };
        return PostAsync<List<AnalysisRecord>>("/v1/get_user_ols_analysis_list", payload);
    }

    // -----------------------------------------------------------------------
    // 6. GetUserStaticAnalysisReportAsync
    //    정적 분석 리포트 이미지의 Presigned S3 URL 목록을 반환합니다.
    //    analysisIndex : GetUserStaticAnalysisListAsync() 결과의 Index (0부터)
    //    URL 유효 시간 : 24시간 (86400초)
    // -----------------------------------------------------------------------
    public Task<StaticReportResponse> GetUserStaticAnalysisReportAsync(
        string userId,
        int    analysisIndex)
    {
        var payload = new
        {
            program_id     = Config.ProgramId,
            security_key   = Config.SecurityKey,
            user_id        = userId,
            analysis_index = analysisIndex
        };
        return PostAsync<StaticReportResponse>("/v1/get_user_static_analysis_report", payload);
    }

    // -----------------------------------------------------------------------
    // 7. GetUserOhsAnalysisReportAsync
    //    OHS 리포트 이미지의 Presigned S3 URL 목록을 반환합니다.
    //    analysisIndex : GetUserOhsAnalysisListAsync() 결과의 Index
    // -----------------------------------------------------------------------
    public Task<OhsReportResponse> GetUserOhsAnalysisReportAsync(
        string userId,
        int    analysisIndex)
    {
        var payload = new
        {
            program_id     = Config.ProgramId,
            security_key   = Config.SecurityKey,
            user_id        = userId,
            analysis_index = analysisIndex
        };
        return PostAsync<OhsReportResponse>("/v1/get_user_ohs_analysis_report", payload);
    }

    // -----------------------------------------------------------------------
    // 8. GetUserOlsAnalysisReportAsync
    //    OLS 리포트 이미지의 Presigned S3 URL 목록을 반환합니다. (좌·우 포함)
    //    analysisIndex : GetUserOlsAnalysisListAsync() 결과의 Index
    // -----------------------------------------------------------------------
    public Task<OlsReportResponse> GetUserOlsAnalysisReportAsync(
        string userId,
        int    analysisIndex)
    {
        var payload = new
        {
            program_id     = Config.ProgramId,
            security_key   = Config.SecurityKey,
            user_id        = userId,
            analysis_index = analysisIndex
        };
        return PostAsync<OlsReportResponse>("/v1/get_user_ols_analysis_report", payload);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 진입점 — 전체 8개 엔드포인트 순서대로 시연
// ─────────────────────────────────────────────────────────────────────────────
class Program
{
    static async Task Main()
    {
        Console.WriteLine(new string('=', 60));
        Console.WriteLine("  Public Moti-Physio API — C# Sample");
        Console.WriteLine(new string('=', 60));

        // HttpClient 는 애플리케이션 수명 동안 재사용해야 합니다.
        // 실제 프로젝트에서는 IHttpClientFactory 또는 DI 로 관리하세요.
        using var httpClient = new HttpClient();
        var client = new MotiPhysioClient(httpClient);

        try
        {
            // ──────────────────────────────────────────────────────────────
            // 1. 회원 목록
            // ──────────────────────────────────────────────────────────────
            Console.WriteLine("\n[1] Fetching member list ...");
            List<UserSummary> users = await client.GetUserListAsync();
            Console.WriteLine($"    → {users.Count} member(s) found");
            foreach (var u in users.Take(3))
                Console.WriteLine($"      {u.UserId} | {u.Name} | {u.Gender} | {u.Birth}");

            string userId = users.Count > 0 ? users[0].UserId : Config.SampleUserId;

            await Task.Delay(200);  // Rate Limit 대응: 200ms 대기

            // ──────────────────────────────────────────────────────────────
            // 2. 회원 상세 정보
            // ──────────────────────────────────────────────────────────────
            Console.WriteLine($"\n[2] Fetching info for user '{userId}' ...");
            UserDetail info = await client.GetUserInfoAsync(userId);
            Console.WriteLine($"    → name: {info.Name}, height: {info.Height} cm, weight: {info.Weight} kg");

            await Task.Delay(200);

            // ──────────────────────────────────────────────────────────────
            // 3. 정적 분석 목록
            // ──────────────────────────────────────────────────────────────
            Console.WriteLine($"\n[3] Fetching Static analysis list for '{userId}' ...");
            List<AnalysisRecord> staticList = await client.GetUserStaticAnalysisListAsync(userId);
            Console.WriteLine($"    → {staticList.Count} record(s)");
            foreach (var r in staticList.Take(3))
                Console.WriteLine($"      index={r.Index}  time={r.MeasurementTime}  ver={r.Version}");

            await Task.Delay(200);

            // ──────────────────────────────────────────────────────────────
            // 4. OHS 분석 목록
            // ──────────────────────────────────────────────────────────────
            Console.WriteLine($"\n[4] Fetching OHS analysis list for '{userId}' ...");
            List<AnalysisRecord> ohsList = await client.GetUserOhsAnalysisListAsync(userId);
            Console.WriteLine($"    → {ohsList.Count} record(s)");

            await Task.Delay(200);

            // ──────────────────────────────────────────────────────────────
            // 5. OLS 분석 목록
            // ──────────────────────────────────────────────────────────────
            Console.WriteLine($"\n[5] Fetching OLS analysis list for '{userId}' ...");
            List<AnalysisRecord> olsList = await client.GetUserOlsAnalysisListAsync(userId);
            Console.WriteLine($"    → {olsList.Count} record(s)");

            await Task.Delay(200);

            // ──────────────────────────────────────────────────────────────
            // 6. 정적 분석 리포트 URL
            // ──────────────────────────────────────────────────────────────
            if (staticList.Count > 0)
            {
                int firstStaticIndex = staticList[0].Index;
                Console.WriteLine($"\n[6] Fetching Static report (analysis_index={firstStaticIndex}) ...");
                StaticReportResponse report = await client.GetUserStaticAnalysisReportAsync(userId, firstStaticIndex);
                int hours = report.UrlExpirationSeconds / 3600;
                Console.WriteLine($"    → URLs valid for {hours} hours");

                // 첫 번째 리포트 타입의 첫 번째 URL 출력
                if (report.Reports.SkeletonResultSheet.Count > 0)
                {
                    string url = report.Reports.SkeletonResultSheet[0].PresignedUrl;
                    Console.WriteLine($"      [skeleton_result_sheet] page 0 → {url[..Math.Min(80, url.Length)]}...");
                }
            }
            else
            {
                Console.WriteLine("\n[6] No Static analysis records — skipping report request");
            }

            await Task.Delay(200);

            // ──────────────────────────────────────────────────────────────
            // 7. OHS 분석 리포트 URL
            // ──────────────────────────────────────────────────────────────
            if (ohsList.Count > 0)
            {
                int firstOhsIndex = ohsList[0].Index;
                Console.WriteLine($"\n[7] Fetching OHS report (analysis_index={firstOhsIndex}) ...");
                OhsReportResponse report = await client.GetUserOhsAnalysisReportAsync(userId, firstOhsIndex);
                Console.WriteLine($"    → {report.Reports.OhsResultSheet.Count} page(s) in ohs_result_sheet");
            }
            else
            {
                Console.WriteLine("\n[7] No OHS analysis records — skipping report request");
            }

            await Task.Delay(200);

            // ──────────────────────────────────────────────────────────────
            // 8. OLS 분석 리포트 URL
            // ──────────────────────────────────────────────────────────────
            if (olsList.Count > 0)
            {
                int firstOlsIndex = olsList[0].Index;
                Console.WriteLine($"\n[8] Fetching OLS report (analysis_index={firstOlsIndex}) ...");
                OlsReportResponse report = await client.GetUserOlsAnalysisReportAsync(userId, firstOlsIndex);
                Console.WriteLine($"    → {report.Reports.OlsResultSheet.Count} page(s) in ols_result_sheet");
            }
            else
            {
                Console.WriteLine("\n[8] No OLS analysis records — skipping report request");
            }

            Console.WriteLine("\nDone.");
        }
        catch (ApiException ex)
        {
            Console.Error.WriteLine($"API error: {ex.Message}");
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Unexpected error: {ex.Message}");
        }
    }
}

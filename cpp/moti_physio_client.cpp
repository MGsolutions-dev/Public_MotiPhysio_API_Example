/**
 * moti_physio_client.cpp
 * ======================
 * Public Moti-Physio API — C++ Sample Client
 *
 * 모든 8개 엔드포인트를 libcurl(HTTP)과 nlohmann/json(JSON 파싱)을 사용해
 * 호출하는 예제입니다.
 *
 * ── 의존성 ──────────────────────────────────────────────────────────────────
 *   libcurl   https://curl.se/libcurl/
 *   nlohmann/json (single-header)  https://github.com/nlohmann/json
 *     → json.hpp 한 파일만 프로젝트에 복사하면 됩니다.
 *
 * ── 빌드 (Linux / macOS) ────────────────────────────────────────────────────
 *   g++ -std=c++17 moti_physio_client.cpp -lcurl -o moti_physio_client
 *   ./moti_physio_client
 *
 * ── 빌드 (Windows / vcpkg) ──────────────────────────────────────────────────
 *   vcpkg install curl nlohmann-json
 *   cl /std:c++17 moti_physio_client.cpp /link libcurl.lib
 *
 * ── 빌드 (CMake) ─────────────────────────────────────────────────────────────
 *   README.md 의 CMakeLists.txt 예시를 참고하세요.
 */

#include <iostream>
#include <string>
#include <stdexcept>
#include <thread>
#include <chrono>

#include <curl/curl.h>          // libcurl
#include "json.hpp"             // nlohmann/json (single-header)

using json = nlohmann::json;

// ─────────────────────────────────────────────────────────────────────────────
// 설정값 — 실제 발급받은 값으로 교체하세요
// ─────────────────────────────────────────────────────────────────────────────
const std::string BASE_URL     = "https://api.motiphysio.com";
const std::string PROGRAM_ID   = "YOUR_PROGRAM_ID";               // 예: "1489"
const std::string SECURITY_KEY = "YOUR_SECURITY_KEY";             // 예: "b25klhxkp36v6fcx7qkz"

// 멤버 목록이 비어 있을 때 리포트 예제에서 사용할 기본 ID
const std::string SAMPLE_USER_ID = "1489-00001";

// ─────────────────────────────────────────────────────────────────────────────
// libcurl 응답 누적 콜백
// curl이 수신한 데이터를 청크 단위로 이 함수에 전달합니다.
// ─────────────────────────────────────────────────────────────────────────────
static size_t write_callback(char* ptr, size_t size, size_t nmemb, void* userdata) {
    auto* buf = reinterpret_cast<std::string*>(userdata);
    buf->append(ptr, size * nmemb);
    return size * nmemb;
}

// ─────────────────────────────────────────────────────────────────────────────
// post()
// endpoint 에 payload(JSON)를 POST 로 전송하고 파싱된 json 객체를 반환합니다.
//
// 예외:
//   std::runtime_error — HTTP 오류 또는 API 업무 오류("error" 키 포함)
// ─────────────────────────────────────────────────────────────────────────────
json post(const std::string& endpoint, const json& payload) {
    std::string url = BASE_URL + endpoint;
    std::string request_body = payload.dump();
    std::string response_body;

    // libcurl 핸들 초기화
    CURL* curl = curl_easy_init();
    if (!curl) {
        throw std::runtime_error("curl_easy_init() failed");
    }

    // Content-Type 헤더 설정
    struct curl_slist* headers = nullptr;
    headers = curl_slist_append(headers, "Content-Type: application/json");

    curl_easy_setopt(curl, CURLOPT_URL,            url.c_str());
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER,     headers);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS,     request_body.c_str());
    curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE,  (long)request_body.size());
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION,  write_callback);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA,      &response_body);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT,        30L);  // 30초 타임아웃

    CURLcode res = curl_easy_perform(curl);

    // 리소스 해제 (RAII 대신 명시적으로 처리)
    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);

    if (res != CURLE_OK) {
        throw std::runtime_error(
            std::string("curl error: ") + curl_easy_strerror(res));
    }

    // JSON 파싱
    json data = json::parse(response_body);

    // API 업무 오류 확인: HTTP 200 이지만 body에 "error" 키가 있는 경우
    if (data.is_object() && data.contains("error")) {
        throw std::runtime_error(
            "API error: " + data["error"].get<std::string>());
    }

    return data;
}

// ─────────────────────────────────────────────────────────────────────────────
// 짧은 대기 함수 (Rate Limit 대응: IP당 1초에 최대 10회)
// ─────────────────────────────────────────────────────────────────────────────
void sleep_ms(int ms) {
    std::this_thread::sleep_for(std::chrono::milliseconds(ms));
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. get_user_list
//    프로그램에 등록된 전체 회원 목록을 반환합니다.
//    start_period / end_period 는 선택 필드로, 0을 전달하면 생략됩니다.
// ─────────────────────────────────────────────────────────────────────────────
json get_user_list(long long start_period = 0, long long end_period = 0) {
    json payload = {
        {"program_id",   PROGRAM_ID},
        {"security_key", SECURITY_KEY}
    };
    // 0이 아닐 때만 선택적 필드 추가
    if (start_period > 0) payload["start_period"] = start_period;
    if (end_period   > 0) payload["end_period"]   = end_period;

    return post("/v1/get_user_list", payload);
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. get_user_info
//    특정 회원의 상세 프로필을 반환합니다.
// ─────────────────────────────────────────────────────────────────────────────
json get_user_info(const std::string& user_id) {
    json payload = {
        {"program_id",   PROGRAM_ID},
        {"security_key", SECURITY_KEY},
        {"user_id",      user_id}
    };
    return post("/v1/get_user_info", payload);
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. get_user_static_analysis_list
//    정적(자세) 분석 기록 목록을 반환합니다.
//    각 항목의 "index" → 리포트 요청 시 analysis_index 로 사용합니다.
// ─────────────────────────────────────────────────────────────────────────────
json get_user_static_analysis_list(
    const std::string& user_id,
    long long start_period = 0,
    long long end_period   = 0)
{
    json payload = {
        {"program_id",   PROGRAM_ID},
        {"security_key", SECURITY_KEY},
        {"user_id",      user_id}
    };
    if (start_period > 0) payload["start_period"] = start_period;
    if (end_period   > 0) payload["end_period"]   = end_period;

    return post("/v1/get_user_static_analysis_list", payload);
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. get_user_ohs_analysis_list
//    OHS(핵심 기능/스쿼트) 분석 기록 목록을 반환합니다.
// ─────────────────────────────────────────────────────────────────────────────
json get_user_ohs_analysis_list(
    const std::string& user_id,
    long long start_period = 0,
    long long end_period   = 0)
{
    json payload = {
        {"program_id",   PROGRAM_ID},
        {"security_key", SECURITY_KEY},
        {"user_id",      user_id}
    };
    if (start_period > 0) payload["start_period"] = start_period;
    if (end_period   > 0) payload["end_period"]   = end_period;

    return post("/v1/get_user_ohs_analysis_list", payload);
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. get_user_ols_analysis_list
//    OLS(균형/한발서기) 분석 기록 목록을 반환합니다.
// ─────────────────────────────────────────────────────────────────────────────
json get_user_ols_analysis_list(
    const std::string& user_id,
    long long start_period = 0,
    long long end_period   = 0)
{
    json payload = {
        {"program_id",   PROGRAM_ID},
        {"security_key", SECURITY_KEY},
        {"user_id",      user_id}
    };
    if (start_period > 0) payload["start_period"] = start_period;
    if (end_period   > 0) payload["end_period"]   = end_period;

    return post("/v1/get_user_ols_analysis_list", payload);
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. get_user_static_analysis_report
//    정적 분석 리포트 이미지의 Presigned S3 URL 목록을 반환합니다.
//    analysis_index : get_user_static_analysis_list() 결과의 "index" 값 (0부터 시작)
//    URL 유효 시간  : 24시간 (86400초)
// ─────────────────────────────────────────────────────────────────────────────
json get_user_static_analysis_report(
    const std::string& user_id,
    int analysis_index)
{
    json payload = {
        {"program_id",     PROGRAM_ID},
        {"security_key",   SECURITY_KEY},
        {"user_id",        user_id},
        {"analysis_index", analysis_index}
    };
    return post("/v1/get_user_static_analysis_report", payload);
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. get_user_ohs_analysis_report
//    OHS 리포트 이미지의 Presigned S3 URL 목록을 반환합니다.
//    analysis_index : get_user_ohs_analysis_list() 결과의 "index" 값
// ─────────────────────────────────────────────────────────────────────────────
json get_user_ohs_analysis_report(
    const std::string& user_id,
    int analysis_index)
{
    json payload = {
        {"program_id",     PROGRAM_ID},
        {"security_key",   SECURITY_KEY},
        {"user_id",        user_id},
        {"analysis_index", analysis_index}
    };
    return post("/v1/get_user_ohs_analysis_report", payload);
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. get_user_ols_analysis_report
//    OLS 리포트 이미지의 Presigned S3 URL 목록을 반환합니다. (좌·우 포함)
//    analysis_index : get_user_ols_analysis_list() 결과의 "index" 값
// ─────────────────────────────────────────────────────────────────────────────
json get_user_ols_analysis_report(
    const std::string& user_id,
    int analysis_index)
{
    json payload = {
        {"program_id",     PROGRAM_ID},
        {"security_key",   SECURITY_KEY},
        {"user_id",        user_id},
        {"analysis_index", analysis_index}
    };
    return post("/v1/get_user_ols_analysis_report", payload);
}

// ─────────────────────────────────────────────────────────────────────────────
// main — 전체 8개 엔드포인트 순서대로 시연
// ─────────────────────────────────────────────────────────────────────────────
int main() {
    // libcurl 전역 초기화 (프로그램 시작 시 한 번만 호출)
    curl_global_init(CURL_GLOBAL_DEFAULT);

    std::cout << std::string(60, '=') << "\n";
    std::cout << "  Public Moti-Physio API — C++ Sample\n";
    std::cout << std::string(60, '=') << "\n";

    try {
        // ──────────────────────────────────────────────────────────────────
        // 1. 회원 목록
        // ──────────────────────────────────────────────────────────────────
        std::cout << "\n[1] Fetching member list ...\n";
        json users = get_user_list();   // 날짜 필터 없음
        std::cout << "    → " << users.size() << " member(s) found\n";
        for (size_t i = 0; i < std::min(users.size(), (size_t)3); ++i) {
            auto& u = users[i];
            std::cout << "      "
                      << u["user_id"].get<std::string>() << " | "
                      << u["name"].get<std::string>()    << " | "
                      << u["gender"].get<std::string>()  << " | "
                      << u["birth"].get<std::string>()   << "\n";
        }

        // 이후 호출에 사용할 user_id 결정
        std::string user_id = users.empty()
            ? SAMPLE_USER_ID
            : users[0]["user_id"].get<std::string>();

        sleep_ms(200);  // Rate Limit 대응: 요청 사이 200ms 대기

        // ──────────────────────────────────────────────────────────────────
        // 2. 회원 상세 정보
        // ──────────────────────────────────────────────────────────────────
        std::cout << "\n[2] Fetching info for user '" << user_id << "' ...\n";
        json info = get_user_info(user_id);
        std::cout << "    → name: " << info["name"].get<std::string>()
                  << ", height: "   << info.value("height", 0.0) << " cm"
                  << ", weight: "   << info.value("weight", 0.0) << " kg\n";

        sleep_ms(200);

        // ──────────────────────────────────────────────────────────────────
        // 3. 정적 분석 목록
        // ──────────────────────────────────────────────────────────────────
        std::cout << "\n[3] Fetching Static analysis list for '" << user_id << "' ...\n";
        json static_list = get_user_static_analysis_list(user_id);
        std::cout << "    → " << static_list.size() << " record(s)\n";
        for (size_t i = 0; i < std::min(static_list.size(), (size_t)3); ++i) {
            auto& r = static_list[i];
            std::cout << "      index=" << r["index"].get<int>()
                      << "  time="      << r["measurement_time"].get<long long>()
                      << "  ver="       << r["version"].get<std::string>() << "\n";
        }

        sleep_ms(200);

        // ──────────────────────────────────────────────────────────────────
        // 4. OHS 분석 목록
        // ──────────────────────────────────────────────────────────────────
        std::cout << "\n[4] Fetching OHS analysis list for '" << user_id << "' ...\n";
        json ohs_list = get_user_ohs_analysis_list(user_id);
        std::cout << "    → " << ohs_list.size() << " record(s)\n";

        sleep_ms(200);

        // ──────────────────────────────────────────────────────────────────
        // 5. OLS 분석 목록
        // ──────────────────────────────────────────────────────────────────
        std::cout << "\n[5] Fetching OLS analysis list for '" << user_id << "' ...\n";
        json ols_list = get_user_ols_analysis_list(user_id);
        std::cout << "    → " << ols_list.size() << " record(s)\n";

        sleep_ms(200);

        // ──────────────────────────────────────────────────────────────────
        // 6. 정적 분석 리포트 URL
        // ──────────────────────────────────────────────────────────────────
        if (!static_list.empty()) {
            int first_static_index = static_list[0]["index"].get<int>();
            std::cout << "\n[6] Fetching Static report (analysis_index="
                      << first_static_index << ") ...\n";
            json report = get_user_static_analysis_report(user_id, first_static_index);
            int expiry_hours = report.value("url_expiration_seconds", 86400) / 3600;
            std::cout << "    → URLs valid for " << expiry_hours << " hours\n";

            // 첫 번째 리포트 타입의 첫 번째 URL만 출력
            auto& reports = report["reports"];
            for (auto it = reports.begin(); it != reports.end(); ++it) {
                if (!it.value().empty()) {
                    std::string url = it.value()[0]["presigned_url"].get<std::string>();
                    std::cout << "      [" << it.key() << "] page 0 → "
                              << url.substr(0, 80) << "...\n";
                    break;
                }
            }
        } else {
            std::cout << "\n[6] No Static analysis records — skipping report request\n";
        }

        sleep_ms(200);

        // ──────────────────────────────────────────────────────────────────
        // 7. OHS 분석 리포트 URL
        // ──────────────────────────────────────────────────────────────────
        if (!ohs_list.empty()) {
            int first_ohs_index = ohs_list[0]["index"].get<int>();
            std::cout << "\n[7] Fetching OHS report (analysis_index="
                      << first_ohs_index << ") ...\n";
            json report = get_user_ohs_analysis_report(user_id, first_ohs_index);
            auto& pages = report["reports"]["ohs_result_sheet"];
            std::cout << "    → " << pages.size() << " page(s) in ohs_result_sheet\n";
        } else {
            std::cout << "\n[7] No OHS analysis records — skipping report request\n";
        }

        sleep_ms(200);

        // ──────────────────────────────────────────────────────────────────
        // 8. OLS 분석 리포트 URL
        // ──────────────────────────────────────────────────────────────────
        if (!ols_list.empty()) {
            int first_ols_index = ols_list[0]["index"].get<int>();
            std::cout << "\n[8] Fetching OLS report (analysis_index="
                      << first_ols_index << ") ...\n";
            json report = get_user_ols_analysis_report(user_id, first_ols_index);
            auto& pages = report["reports"]["ols_result_sheet"];
            std::cout << "    → " << pages.size() << " page(s) in ols_result_sheet\n";
        } else {
            std::cout << "\n[8] No OLS analysis records — skipping report request\n";
        }

        std::cout << "\nDone.\n";

    } catch (const std::exception& ex) {
        std::cerr << "Error: " << ex.what() << "\n";
        curl_global_cleanup();
        return 1;
    }

    curl_global_cleanup();  // libcurl 전역 정리
    return 0;
}

cmake_minimum_required(VERSION 3.15)
project(MotiPhysioClient CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ── nlohmann/json ────────────────────────────────────────────────────────────
# json.hpp 를 이 디렉토리에 복사한 뒤 빌드하거나,
# FetchContent 로 자동 다운로드합니다.
include(FetchContent)
FetchContent_Declare(
    nlohmann_json
    URL https://github.com/nlohmann/json/releases/download/v3.11.3/json.hpp
    DOWNLOAD_NO_EXTRACT TRUE
    SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}
)
FetchContent_MakeAvailable(nlohmann_json)

# ── libcurl ──────────────────────────────────────────────────────────────────
find_package(CURL REQUIRED)

add_executable(moti_physio_client moti_physio_client.cpp)

target_include_directories(moti_physio_client PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(moti_physio_client PRIVATE CURL::libcurl)

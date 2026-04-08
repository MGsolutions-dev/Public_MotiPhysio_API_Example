# C++ Sample — Public Moti-Physio API

## 의존성

| 라이브러리 | 용도 | 획득 방법 |
|-----------|------|----------|
| **libcurl** | HTTP POST 요청 | 패키지 매니저 또는 공식 사이트 |
| **nlohmann/json** | JSON 직렬화/역직렬화 | 단일 헤더 파일(`json.hpp`) 복사 |

### nlohmann/json 설치

프로젝트 폴더에 `json.hpp` 한 파일만 복사하면 됩니다.

```bash
# curl로 직접 다운로드
curl -Lo json.hpp \
  https://github.com/nlohmann/json/releases/download/v3.11.3/json.hpp
```

또는 CMake를 사용하면 자동으로 다운로드됩니다 (CMakeLists.txt 참고).

---

## 빌드 방법

### 방법 1 — g++ 직접 컴파일 (Linux / macOS)

```bash
# libcurl 설치 (Ubuntu/Debian)
sudo apt install libcurl4-openssl-dev

# libcurl 설치 (macOS)
brew install curl

# 컴파일 & 실행
g++ -std=c++17 moti_physio_client.cpp -lcurl -o moti_physio_client
./moti_physio_client
```

### 방법 2 — CMake (플랫폼 공통)

```bash
mkdir build && cd build
cmake ..
cmake --build .
./moti_physio_client   # Linux/macOS
moti_physio_client.exe # Windows
```

### 방법 3 — vcpkg (Windows)

```bash
vcpkg install curl nlohmann-json
cl /std:c++17 moti_physio_client.cpp /link libcurl.lib
```

---

## 설정

`moti_physio_client.cpp` 상단의 세 상수를 수정하세요:

```cpp
const std::string BASE_URL     = "https://api.motiphysio.com";
const std::string PROGRAM_ID   = "YOUR_PROGRAM_ID";
const std::string SECURITY_KEY = "YOUR_SECURITY_KEY";
```

---

## 파일 구조

```
cpp/
├── moti_physio_client.cpp   # 전체 8개 엔드포인트 구현
├── CMakeLists.txt           # CMake 빌드 설정
├── json.hpp                 # nlohmann/json (별도 다운로드 필요)
└── README.md
```

> `json.hpp`는 저장소에 포함되지 않습니다. 위 안내에 따라 직접 다운로드하세요.

---

## 참고 사항

- `curl_global_init` / `curl_global_cleanup` 은 프로그램 생애 동안 각 한 번씩만 호출합니다.
- API 업무 오류(`"error"` 키)는 `std::runtime_error` 로 변환되어 `catch` 블록에서 처리됩니다.
- Rate Limit 대응을 위해 요청 사이 200ms 대기가 포함되어 있습니다.
- 프로덕션 환경에서는 `CURL*` 핸들을 RAII 래퍼로 감싸는 것을 권장합니다.

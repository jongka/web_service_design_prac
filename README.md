# 웹서비스설계 실습

Spring Boot 실습 예제의 v3 상품 API를 확장, HTTP 메소드별 API 2개씩 총 8개, 요청 로그 미들웨어, 다양한 HTTP 상태 코드, 공통 오류 응답을 구현했습니다.

## 개발 환경

- Java 21 (Gradle Java toolchain)
- Spring Boot 4.1.1 / Spring Web MVC
- Gradle Wrapper
- JUnit / Spring MockMvc
- 데이터 저장: 메모리의 `LinkedHashMap` (외부 DB 미사용)

서버를 재시작하면 상품과 ID 순번이 초기화됩니다.

## 실행 방법

### IntelliJ IDEA

1. 이 폴더를 Gradle 프로젝트로 엽니다.
2. Project SDK 및 Gradle JVM을 JDK 21로 설정합니다.
3. `WebServiceDesignHw1Application`의 `main` 메소드를 실행합니다.
4. `http://localhost:8080/api/v3/items`에 접속합니다. 초기 응답은 `[]`입니다.

### 터미널

프로젝트 최상위 폴더에서 실행합니다. JDK 21을 설치하고 `JAVA_HOME`을 해당 JDK 경로로 설정해야 합니다.

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

최초 실행에는 Gradle 인터넷 연결이 필요합니다.
루트 주소 `/`에는 화면을 구현하지 않았습니다. API 주소로 요청해야 합니다.

## API 목록

기본 주소: `http://localhost:8080/api/v3/items`

| 메소드 | 경로 | 기능 | 성공 코드 |
|---|---|---|---|
| GET | `/api/v3/items` | 전체 상품 조회 | 200 |
| GET | `/api/v3/items/{id}` | 단건 상품 조회 | 200 |
| POST | `/api/v3/items` | 단건 상품 생성 | 201 |
| POST | `/api/v3/items/bulk` | 상품 일괄 생성 | 201 |
| PUT | `/api/v3/items/{id}` | 단건 상품 수정 | 200 |
| PUT | `/api/v3/items/bulk/prices` | 상품 가격 일괄 수정 | 200 |
| DELETE | `/api/v3/items/{id}` | 단건 상품 삭제 | 204 |
| DELETE | `/api/v3/items/bulk?ids=1,2` | 선택한 상품 일괄 삭제 | 204 |

POST / GET / PUT / DELETE 각각 2개씩 구현했습니다. JSON 요청은 `Content-Type: application/json` 헤더를 사용합니다.

## 요청과 성공 응답 예시

### 단건 생성

```http
POST /api/v3/items
Content-Type: application/json

{
  "name": "노트북",
  "price": 1000000
}
```

```http
HTTP/1.1 201 Created
Location: /api/v3/items/1
Content-Type: application/json

{
  "id": 1,
  "name": "노트북",
  "price": 1000000
}
```

### 일괄 생성

`POST /api/v3/items/bulk` 요청 본문:

```json
[
  {"name": "마우스", "price": 30000},
  {"name": "키보드", "price": 50000}
]
```

생성한 상품 배열과 `201`을 반환합니다. Location은 목록 주소 `/api/v3/items`입니다. 빈 배열 또는 잘못된 상품이 포함되면 저장하지 않고 `400`을 반환합니다.

### 단건 수정

`PUT /api/v3/items/1` 요청 본문:

```json
{
  "name": "게이밍 노트북",
  "price": 1500000
}
```

예제의 단건 PUT은 이름 또는 가격만 보내도 해당 필드만 수정합니다. 둘 다 생략하면 `400`입니다. 성공하면 수정된 상품 객체를 반환합니다.

### 일괄 가격 수정

`PUT /api/v3/items/bulk/prices` 요청 본문 (ID는 실제 생성 결과에 맞춰 변경):

```json
[
  {"id": 2, "price": 25000},
  {"id": 3, "price": 45000}
]
```

모든 ID와 가격을 확인한 뒤 수정합니다. 중복 ID 또는 잘못된 입력은 `400`, 존재하지 않는 ID는 `404`이며, 실패 시 일부 상품만 수정하지 않습니다.

### 조회와 삭제

- 전체 조회는 상품 객체 배열, 단건 조회는 상품 객체를 반환합니다.
- 단건 삭제: `DELETE /api/v3/items/1`
- 일괄 삭제: `DELETE /api/v3/items/bulk?ids=2,3`
- 삭제 성공은 `204 No Content`이며 응답 본문이 없습니다.
- 일괄 삭제는 모든 ID가 존재하는지 확인한 뒤 삭제합니다. 중복 ID는 `400`, 없는 ID는 `404`입니다.

## 응답 코드와 오류 형식

강의 자료의 성공 응답 예시처럼 객체 또는 배열을 직접 반환합니다. 오류는 `GlobalExceptionHandler`에서 아래 형식으로 통일합니다.

```json
{
  "statusCode": 404,
  "message": "상품을 찾을 수 없습니다."
}
```

| 범주 | 코드 | 사용 상황 |
|---|---|---|
| 2xx | 200 OK | 조회·수정 성공 |
| 2xx | 201 Created | 생성 성공 |
| 2xx | 204 No Content | 삭제 성공 |
| 4xx | 400 Bad Request | 잘못된 입력·JSON·ID 형식 |
| 4xx | 404 Not Found | 대상 상품 없음 |
| 5xx | 500 Internal Server Error | 예상하지 못한 서버 오류 |
| 5xx | 503 Service Unavailable | 실습용 서비스 일시 이용 불가 시연 |

5xx 응답은 내부 오류 내용을 노출하지 않습니다.

```json
{
  "statusCode": 500,
  "message": "서버 처리 중 오류가 발생했습니다."
}
```

### 500 / 503 재현 방법

과제 확인을 위한 실습용 기능입니다. 기존 API 요청에 `X-Demo-Error` 헤더를 추가합니다. 실제 장애를 발생시키거나 API를 별도로 추가하지 않습니다.

```http
GET http://localhost:8080/api/v3/items
X-Demo-Error: 500
```

```http
GET http://localhost:8080/api/v3/items
X-Demo-Error: 503
```

각각 `500`, `503`과 공통 오류 JSON을 반환합니다. 헤더를 생략하면 정상 처리됩니다.

## 요청 로그 미들웨어

`LoggingInterceptor`를 `WebConfig`에서 `/api/**`에 등록했습니다. 서버 콘솔(IntelliJ Run 창)에 요청 메소드, URI, 쿼리 문자열, 완료 상태 코드를 출력합니다.

```text
[LoggingInterceptor] 요청 시작: GET /api/v3/items
[LoggingInterceptor] 요청 완료, status=200
```

실습용 오류 헤더는 `preHandle`에서 예외를 발생시키므로 해당 요청은 시작 로그를 출력하지만 같은 인터셉터의 완료 로그는 출력하지 않습니다. 일반 요청은 시작·완료 로그를 확인할 수 있습니다.

## 실행 및 테스트

### 수동 HTTP 테스트

1. 서버를 실행합니다.
2. IntelliJ HTTP Client에서 프로젝트 최상위의 `requests.http`를 엽니다.
3. 1~8번 요청을 순서대로 실행합니다. 생성된 ID는 응답 스크립트가 자동 저장합니다.
4. 9~13번 요청으로 입력 오류, 없는 상품, 500, 503, 잘못된 JSON을 확인합니다.
5. 응답 코드·본문·Location 헤더와 서버 콘솔 로그를 확인합니다.

서버를 재시작했다면 1번부터 다시 실행하세요. HTTP Client를 사용할 수 없는 환경에서는 Postman 등으로 같은 요청을 보내고 생성된 ID를 직접 입력하면 됩니다.

### 자동 테스트

```powershell
.\gradlew.bat test
```

테스트 구성:

- `WebServiceDesignHw1ApplicationTests`: 애플리케이션 컨텍스트 시작 확인
- `ItemController3Tests`: MockMvc로 API 8개 흐름, 공통 오류 응답(400·404·500·503), 일괄 처리 실패 시 데이터 보존 확인

## 주요 파일

패키지 기준: `src/main/java/kr/ac/jbnu/bjh/web_service_design_hw1`

| 파일 | 역할 |
|---|---|
| `WebServiceDesignHw1Application.java` | 서버 실행 진입점 |
| `api/v3/ItemController3.java` | 상품 API 8개 및 메모리 저장 |
| `api/dto/ItemDto.java` | 상품 응답 데이터 |
| `api/request/ItemCreateRequest.java` | 생성 요청 |
| `api/request/ItemUpdateRequest.java` | 단건 수정 요청 |
| `api/request/ItemPriceUpdateRequest.java` | 일괄 가격 수정 요청 |
| `config/LoggingInterceptor.java` | 요청 로그 및 실습용 오류 시연 |
| `config/WebConfig.java` | 인터셉터 등록 |
| `config/GlobalExceptionHandler.java` | 공통 오류 응답 |

## 제출 자료

-  GitHub 저장소 링크
- 서버 실행 화면, API 8개 실행 결과, 400·404·500·503 응답, 요청 로그 화면 캡처
- `.idea`, `.gradle`, `build` 등 생성 파일은 업로드 대상에서 제외

제공된 Spring Boot v3 예제를 바탕으로 확장했으며, 응답 형식과 오류 처리는 03주차 웹서비스설계 강의 자료를 참고했습니다.
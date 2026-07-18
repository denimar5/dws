# DWS Backend Challenge — Spec-Driven Development

## 1. Purpose

Build a production-quality Java backend that consumes the external Bands API, exposes a REST API for multiple consumers, and implements a caching layer.

This repository is part of a backend recruitment challenge. The implementation must demonstrate:

- Clean Code
- Maintainability
- Testability
- Scalability awareness
- Performance awareness
- Clear architectural boundaries
- Effective use of AI-assisted software development
- Small, working, incremental deliveries

The solution must prioritize working core functionality over unnecessary complexity.

---

## 2. Source Requirement

External provider:

```text
GET https://bands-api.vercel.app/api/bands
```

The provider returns a JSON array of bands.

Expected provider fields include:

```json
{
  "id": "uuid",
  "name": "Band name",
  "image": "https://...",
  "genre": "rock",
  "biography": "Biography text",
  "numPlays": 123456,
  "albums": [
    "album-id-1",
    "album-id-2"
  ]
}
```

Do not modify, scrape, or depend on undocumented endpoints.

---

## 3. Timebox and Development Strategy

The original challenge has a four-hour delivery expectation.

Implement using small, verifiable increments.

Required order:

1. Bootstrap and health check
2. External API integration
3. List bands
4. Search and sorting
5. Band details
6. Cache
7. Error handling
8. Tests
9. CORS
10. i18n
11. Documentation and optional polish

Do not start optional items before all mandatory acceptance criteria are passing.

After each phase:

```bash
./mvnw clean test
```

The application must remain compilable and testable after every phase.

---

## 4. Technology Stack

Use:

- Java 17
- Spring Boot 3.x
- Maven Wrapper
- Spring Web MVC
- Spring HTTP Interface backed by RestClient
- Spring Cache
- Caffeine
- Spring Boot Validation
- Spring Boot Actuator
- JUnit 5
- Mockito
- MockWebServer or WireMock
- Spring Boot Test
- Springdoc OpenAPI as an optional bonus
- Dockerfile as an optional bonus

Do not use:

- RestTemplate
- Reactive WebFlux unless the entire application is reactive
- Database
- Kafka
- Redis
- Kubernetes
- Authentication
- Persistence
- Unnecessary code generation frameworks
- Lombok unless its use is clearly justified

Use Java records where appropriate.

---

## 5. Architecture

Implement a pragmatic hexagonal architecture.

Expected dependency direction:

```text
Inbound Adapter
    -> Application Port In
        -> Application Service
            -> Application Port Out
                -> Outbound Adapter
                    -> External Bands API
```

The domain and application layers must not depend on:

- Spring MVC
- RestClient
- HTTP response classes
- JSON annotations from the external provider
- Caffeine
- ControllerAdvice

Suggested package structure:

```text
src/main/java/com/dws/isobarfm
├── IsobarFmApplication.java
├── domain
│   ├── model
│   │   └── Band.java
│   ├── enum
│   │   ├── BandSort.java
│   │   └── SortDirection.java
│   └── exception
│       └── BandNotFoundException.java
├── application
│   ├── port
│   │   ├── in
│   │   │   ├── FindBandsUseCase.java
│   │   │   └── GetBandDetailsUseCase.java
│   │   └── out
│   │       └── LoadBandsPort.java
│   └── service
│       └── BandService.java
├── adapter
│   ├── in
│   │   └── web
│   │       ├── BandController.java
│   │       ├── dto
│   │       │   ├── BandSummaryResponse.java
│   │       │   ├── BandDetailResponse.java
│   │       │   ├── BandListResponse.java
│   │       │   └── ApiErrorResponse.java
│   │       ├── mapper
│   │       │   └── BandWebMapper.java
│   │       └── error
│   │           └── GlobalExceptionHandler.java
│   └── out
│       └── bandsapi
│           ├── BandsApiAdapter.java
│           ├── BandsApiClient.java
│           ├── BandsApiResponse.java
│           ├── BandsApiMapper.java
│           └── exception
│               ├── BandsProviderUnavailableException.java
│               └── BandsProviderInvalidResponseException.java
└── config
    ├── CacheConfiguration.java
    ├── CorsConfiguration.java
    ├── HttpClientConfiguration.java
    ├── MessageConfiguration.java
    └── properties
        ├── BandsApiProperties.java
        ├── CacheProperties.java
        └── CorsProperties.java
```

The exact names may vary, but architectural boundaries must remain clear.

---

## 6. Domain Model

Create an immutable domain model:

```java
public record Band(
    UUID id,
    String name,
    URI image,
    String genre,
    String biography,
    long numPlays,
    List<UUID> albumIds
) {}
```

Requirements:

- Defensively copy collections.
- Do not expose external provider DTOs outside the outbound adapter.
- Do not put Spring annotations in the domain model.
- Do not return mutable lists.

---

## 7. Input Ports

### 7.1 FindBandsUseCase

Responsibilities:

- Load bands through the output port.
- Filter by partial band name.
- Search must be case-insensitive.
- Search must ignore leading and trailing whitespace.
- Sort alphabetically or by popularity.
- Apply ascending or descending direction.
- Return an immutable result.

Suggested contract:

```java
public interface FindBandsUseCase {
    List<Band> find(String search, BandSort sort, SortDirection direction);
}
```

### 7.2 GetBandDetailsUseCase

Responsibilities:

- Find a band by UUID.
- Throw `BandNotFoundException` when absent.

Suggested contract:

```java
public interface GetBandDetailsUseCase {
    Band getById(UUID id);
}
```

---

## 8. Output Port

Create:

```java
public interface LoadBandsPort {
    List<Band> loadAll();
}
```

The application layer must depend only on this port.

The external API implementation belongs to the outbound adapter.

---

## 9. External API Client

Use Spring HTTP Interface backed by `RestClient`.

Suggested client:

```java
public interface BandsApiClient {

    @GetExchange("/api/bands")
    List<BandsApiResponse> getBands();
}
```

Configure it through `HttpServiceProxyFactory`.

The base URL and timeouts must be externalized.

Example:

```yaml
clients:
  bands-api:
    base-url: ${BANDS_API_BASE_URL:https://bands-api.vercel.app}
    connect-timeout: ${BANDS_API_CONNECT_TIMEOUT:2s}
    read-timeout: ${BANDS_API_READ_TIMEOUT:5s}
```

Use typed configuration properties:

```java
@ConfigurationProperties(prefix = "clients.bands-api")
public record BandsApiProperties(
    URI baseUrl,
    Duration connectTimeout,
    Duration readTimeout
) {}
```

### 9.1 Error Translation

The outbound adapter must translate technical HTTP client exceptions into application-specific exceptions.

Translate:

- Connection failure -> `BandsProviderUnavailableException`
- Read timeout -> `BandsProviderUnavailableException`
- Provider HTTP 5xx -> `BandsProviderUnavailableException`
- Invalid or incompatible provider payload -> `BandsProviderInvalidResponseException`
- Provider HTTP 4xx -> appropriate provider integration exception

Do not allow `RestClientException`, socket exceptions, or provider DTO exceptions to escape the outbound adapter.

---

## 10. Cache

Implement an in-memory cache with Caffeine.

Cache the complete list returned by the external provider.

Do not create separate provider calls for every search or sort combination.

Expected flow:

```text
First request
    -> external provider
    -> map response
    -> cache complete list

Following requests
    -> cache
    -> filter and sort in application memory
```

Suggested behavior:

```java
@Cacheable(cacheNames = "bands", key = "'all'", sync = true)
public List<Band> loadAll() {
    ...
}
```

Configuration:

```yaml
app:
  cache:
    bands:
      ttl: ${BANDS_CACHE_TTL:10m}
      maximum-size: ${BANDS_CACHE_MAXIMUM_SIZE:10}
```

Requirements:

- Cache TTL must be configurable.
- Cache size must be configurable.
- Do not cache exceptions.
- Return immutable cached values.
- Include a test proving repeated use-case calls trigger only one provider call.
- `sync = true` is recommended to reduce duplicate concurrent cache loads.

---

## 11. REST API

Base path:

```text
/api/v1/bands
```

### 11.1 List, Search and Sort

```http
GET /api/v1/bands
```

Optional query parameters:

```text
search
sort
direction
```

Supported sort values:

```text
name
popularity
```

Supported direction values:

```text
asc
desc
```

Defaults:

```text
sort=name
direction=asc
```

Examples:

```http
GET /api/v1/bands
GET /api/v1/bands?search=pink
GET /api/v1/bands?sort=name&direction=asc
GET /api/v1/bands?sort=popularity&direction=desc
GET /api/v1/bands?search=rock&sort=popularity&direction=desc
```

Response:

```json
{
  "count": 1,
  "items": [
    {
      "id": "00000000-0000-0000-0000-000000000000",
      "name": "Pink Floyd",
      "image": "https://...",
      "genre": "progressive rock",
      "numPlays": 284212
    }
  ]
}
```

Rules:

- No results must return `200 OK` with `count = 0` and an empty list.
- Invalid sort values must return `400 Bad Request`.
- Search must be case-insensitive.
- Search must use partial matching.
- Responses must not expose biography in list results.

### 11.2 Band Details

```http
GET /api/v1/bands/{id}
```

Response:

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "name": "Pink Floyd",
  "image": "https://...",
  "genre": "progressive rock",
  "biography": "...",
  "numPlays": 284212,
  "albumIds": [
    "00000000-0000-0000-0000-000000000001"
  ]
}
```

Rules:

- Unknown UUID -> `404 Not Found`
- Malformed UUID -> `400 Bad Request`
- Return album identifiers only because no documented album-details endpoint was provided.

---

## 12. HTTP Error Contract

All API errors must use a stable structure:

```json
{
  "timestamp": "2026-07-18T17:30:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "BAND_NOT_FOUND",
  "message": "Band with identifier ... was not found.",
  "path": "/api/v1/bands/..."
}
```

Use `@RestControllerAdvice`.

Required mappings:

| Situation | HTTP Status | Code |
|---|---:|---|
| Band not found | 404 | `BAND_NOT_FOUND` |
| Malformed UUID | 400 | `INVALID_REQUEST` |
| Invalid sort | 400 | `INVALID_SORT` |
| Invalid direction | 400 | `INVALID_DIRECTION` |
| Provider unavailable | 503 | `BANDS_PROVIDER_UNAVAILABLE` |
| Provider invalid response | 502 | `BANDS_PROVIDER_INVALID_RESPONSE` |
| Unexpected error | 500 | `INTERNAL_SERVER_ERROR` |

Requirements:

- Do not expose stack traces.
- Do not expose provider internals.
- Log unexpected exceptions with stack trace.
- Log expected business exceptions without unnecessary stack traces.
- Error codes must never be translated.
- Only human-readable messages are localized.

---

## 13. Internationalization

Support:

- English
- Brazilian Portuguese
- Spanish

Use:

```text
Accept-Language
```

Files:

```text
src/main/resources/
├── messages.properties
├── messages_en.properties
├── messages_pt_BR.properties
└── messages_es.properties
```

Required message keys:

```properties
error.band.not-found=
error.invalid-request=
error.invalid-sort=
error.invalid-direction=
error.provider.unavailable=
error.provider.invalid-response=
error.internal=
```

Rules:

- English is the default language.
- Unsupported languages fall back to English.
- API error `code` remains language-neutral.
- `Accept-Language: pt-BR` returns Portuguese messages.
- `Accept-Language: es` returns Spanish messages.
- Include at least one MVC test for each supported locale.

---

## 14. CORS

Configure CORS globally for:

```text
/api/**
```

CORS is needed for browser-based frontend consumers.

Native mobile applications and backend-to-backend calls do not depend on browser CORS enforcement.

Allowed methods:

```text
GET
OPTIONS
```

Allowed headers:

```text
Accept
Accept-Language
Content-Type
```

Credentials:

```text
false
```

Externalized origins:

```yaml
app:
  cors:
    allowed-origins:
      - ${CORS_ALLOWED_ORIGIN_1:http://localhost:3000}
      - ${CORS_ALLOWED_ORIGIN_2:http://localhost:4200}
      - ${CORS_ALLOWED_ORIGIN_3:http://localhost:5173}
```

Requirements:

- Do not add `@CrossOrigin` to individual controllers.
- Do not hardcode production domains in Java.
- Do not use wildcard origins with credentials.
- Add a preflight test for an allowed origin.
- Add a test that an unconfigured origin is not accepted.

---

## 15. Observability

Include Spring Boot Actuator.

Required endpoint:

```http
GET /actuator/health
```

Expose only the minimum necessary actuator endpoints.

Suggested configuration:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

Logging requirements:

- Log provider call failures.
- Log provider response time at debug level.
- Do not log full provider payloads.
- Do not log biographies or other large content.
- Do not log stack traces for normal `404` business outcomes.

Optional:

- Micrometer cache metrics
- HTTP client metrics
- Correlation ID

Do not implement optional observability before mandatory requirements pass.

---

## 16. Tests

Follow a test pyramid.

### 16.1 Domain and Application Unit Tests

Test `BandService` without Spring context.

Mandatory cases:

1. Return all bands.
2. Filter by partial name.
3. Search is case-insensitive.
4. Trim search input.
5. Sort by name ascending.
6. Sort by name descending.
7. Sort by popularity ascending.
8. Sort by popularity descending.
9. Get existing band by ID.
10. Throw `BandNotFoundException` for unknown ID.
11. Return immutable results.
12. Handle null or blank search as no filter.

Mock only `LoadBandsPort`.

### 16.2 Outbound Adapter Integration Tests

Use MockWebServer or WireMock.

Mandatory cases:

1. Deserialize valid provider response.
2. Map provider DTO to domain.
3. Provider 500 becomes provider-unavailable exception.
4. Connection or read timeout becomes provider-unavailable exception.
5. Invalid payload becomes invalid-response exception.
6. Base URL is configurable.

Do not call the real internet in automated tests.

### 16.3 Cache Tests

Mandatory cases:

1. Two calls use one provider request.
2. Different searches still use the same cached complete list.
3. Provider exception is not cached.
4. Cached list cannot be externally mutated.

### 16.4 Controller Tests

Use MockMvc.

Mandatory cases:

1. `GET /api/v1/bands` returns `200`.
2. Search query returns correct count.
3. Invalid sort returns `400`.
4. Unknown band returns `404`.
5. Provider unavailable returns `503`.
6. Provider invalid response returns `502`.
7. Portuguese error message.
8. Spanish error message.
9. English fallback.
10. Allowed CORS preflight.
11. Unknown CORS origin is not accepted.

### 16.5 Context Test

Include:

```java
@SpringBootTest
class IsobarFmApplicationTests {
    @Test
    void contextLoads() {}
}
```

---

## 17. Clean Code Rules

Apply the following:

- Prefer constructor injection.
- No field injection.
- No static service locator.
- No business logic in controllers.
- No HTTP client calls in controllers.
- No external DTOs in application services.
- No Spring annotations in the domain.
- Use meaningful names.
- Keep methods focused.
- Avoid premature abstractions.
- Avoid generic names such as `Utils`, `Helper`, or `Manager`.
- Avoid boolean parameters when an enum expresses intent better.
- Use immutable data structures.
- Avoid returning `null`.
- Use Optional only when it improves API clarity.
- Centralize mappings.
- Validate configuration at startup.
- Keep public APIs documented.

---

## 18. Configuration Files

Create:

```text
application.yml
application-test.yml
```

Recommended `application.yml`:

```yaml
spring:
  application:
    name: isobar-fm-api
  messages:
    basename: messages
    fallback-to-system-locale: false

clients:
  bands-api:
    base-url: ${BANDS_API_BASE_URL:https://bands-api.vercel.app}
    connect-timeout: ${BANDS_API_CONNECT_TIMEOUT:2s}
    read-timeout: ${BANDS_API_READ_TIMEOUT:5s}

app:
  cache:
    bands:
      ttl: ${BANDS_CACHE_TTL:10m}
      maximum-size: ${BANDS_CACHE_MAXIMUM_SIZE:10}
  cors:
    allowed-origins:
      - ${CORS_ALLOWED_ORIGIN_1:http://localhost:3000}
      - ${CORS_ALLOWED_ORIGIN_2:http://localhost:4200}
      - ${CORS_ALLOWED_ORIGIN_3:http://localhost:5173}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

Tests must override the provider URL with MockWebServer or WireMock.

---

## 19. README

Create a professional `README.md` containing:

1. Project purpose
2. Challenge summary
3. Architecture diagram
4. Technology choices
5. How to run
6. How to test
7. API examples
8. Cache strategy
9. Error handling strategy
10. i18n usage
11. CORS configuration
12. Configuration environment variables
13. Trade-offs
14. Known limitations
15. AI-assisted development disclosure

Required commands:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

Example requests:

```bash
curl "http://localhost:8080/api/v1/bands"

curl "http://localhost:8080/api/v1/bands?search=pink"

curl "http://localhost:8080/api/v1/bands?sort=popularity&direction=desc"

curl \
  -H "Accept-Language: pt-BR" \
  "http://localhost:8080/api/v1/bands/00000000-0000-0000-0000-000000000000"
```

### 19.1 AI-Assisted Development Disclosure

Include a section similar to:

```text
This project was developed using a spec-driven workflow with Claude Code as an implementation assistant. Architectural decisions, requirements, acceptance criteria, review, testing strategy, and final validation remained under developer ownership.

Claude Code was used to:
- scaffold code from explicit specifications;
- accelerate repetitive implementation;
- suggest test cases;
- support refactoring;
- review consistency across layers.

All generated code was reviewed, tested, and adjusted before acceptance.
```

This must not imply that the developer delegated responsibility for correctness.

---

## 20. Optional OpenAPI

Only after mandatory tests pass, add Springdoc OpenAPI.

Expected endpoints:

```text
/swagger-ui.html
/v3/api-docs
```

Document:

- Query parameters
- Supported sort values
- Error responses
- `Accept-Language`
- Example responses

Do not let OpenAPI annotations pollute domain or application layers.

---

## 21. Optional Dockerfile

Only after mandatory tests pass.

Requirements:

- Multi-stage build
- Non-root runtime user
- Small runtime image
- Expose port 8080
- Health check optional

Example build:

```bash
docker build -t isobar-fm-api .
docker run --rm -p 8080:8080 isobar-fm-api
```

---

## 22. Non-Goals

Do not implement:

- Database persistence
- CRUD for bands
- User authentication
- Authorization
- Message brokers
- Distributed cache
- Album metadata enrichment through undocumented endpoints
- Frontend
- Mobile application
- Cloud deployment as part of the core four-hour scope
- Kubernetes manifests
- Complex retry policies
- Circuit breaker before the core is complete

---

## 23. Incremental Implementation Plan

### Phase 1 — Bootstrap

Create the Spring Boot application with:

- Java 17
- Maven Wrapper
- Spring Web
- Validation
- Actuator
- Test dependencies

Acceptance criteria:

- Application starts.
- `/actuator/health` returns `UP`.
- `./mvnw clean test` passes.

### Phase 2 — Domain and Ports

Create:

- Domain model
- Sort enums
- Input ports
- Output port
- Domain exception

Acceptance criteria:

- No Spring dependency in domain.
- Application compiles.
- Unit tests for sorting and search behavior are started.

### Phase 3 — External Integration

Create:

- Typed properties
- RestClient configuration
- Spring HTTP Interface
- Provider DTO
- Provider mapper
- Outbound adapter
- Technical error translation

Acceptance criteria:

- Valid provider JSON is mapped correctly.
- Provider tests use MockWebServer or WireMock.
- No real internet dependency in tests.

### Phase 4 — Application Service

Implement:

- Search
- Sorting
- Details by ID
- Not-found behavior

Acceptance criteria:

- All application unit tests pass.
- Application service depends only on output port.

### Phase 5 — REST API

Implement:

- List endpoint
- Details endpoint
- Web DTOs
- Web mapper
- Input validation

Acceptance criteria:

- Controller contains no business logic.
- Contract matches this specification.
- MockMvc happy-path tests pass.

### Phase 6 — Cache

Implement:

- Caffeine configuration
- Configurable TTL
- Cache on complete provider list
- Cache tests

Acceptance criteria:

- Repeated calls trigger one provider request.
- Different search queries reuse the same cached list.
- Exceptions are not cached.

### Phase 7 — Error Handling

Implement:

- Standard error response
- ControllerAdvice
- Status mapping
- Structured logging

Acceptance criteria:

- Required error statuses and codes are covered by tests.
- No technical client exception leaks through the REST API.

### Phase 8 — i18n

Implement:

- English
- Portuguese
- Spanish
- Header-based locale resolution

Acceptance criteria:

- Error messages change according to `Accept-Language`.
- Codes remain stable.
- Unsupported locale falls back to English.

### Phase 9 — CORS

Implement:

- Global CORS configuration
- Externalized origins
- GET and OPTIONS only
- No credentials

Acceptance criteria:

- Allowed preflight succeeds.
- Unknown origin is not accepted.

### Phase 10 — Documentation and Polish

Implement:

- README
- Architecture diagram
- Curl examples
- Trade-offs
- AI-assisted development disclosure
- Optional OpenAPI
- Optional Dockerfile

Acceptance criteria:

- A reviewer can clone, test, and run the project using README instructions.
- `./mvnw clean test` passes.

---

## 24. Definition of Done

The project is done when:

- The application compiles.
- All automated tests pass.
- The real provider can be consumed at runtime.
- Automated tests do not use the real provider.
- Listing works.
- Search works.
- Sorting works.
- Details work.
- Cache is demonstrably active.
- Errors have stable contracts.
- i18n works.
- CORS works for configured browser origins.
- Configuration is externalized.
- README explains architecture and trade-offs.
- No unnecessary infrastructure was added.
- No known failing test is ignored.

---

## 25. Claude Code Execution Instructions

Act as a senior Java engineer implementing this specification.

Rules:

1. Read the full specification before changing files.
2. Inspect the repository before creating code.
3. Do not invent undocumented provider endpoints.
4. Implement one phase at a time.
5. After each phase, run:
   ```bash
   ./mvnw clean test
   ```
6. Fix failures before continuing.
7. Keep the solution inside the defined scope.
8. Preserve hexagonal dependency direction.
9. Do not place business logic in controllers.
10. Do not expose provider DTOs outside the outbound adapter.
11. Do not use RestTemplate.
12. Do not add WebFlux unless the application becomes reactive end-to-end.
13. Do not add a database.
14. Do not add security.
15. Do not silently swallow exceptions.
16. Do not use the real provider in automated tests.
17. Prefer clear code over excessive abstraction.
18. Before finishing, review:
    - package boundaries;
    - immutability;
    - error contracts;
    - cache behavior;
    - CORS behavior;
    - localized messages;
    - README accuracy.
19. Provide a final implementation report listing:
    - files created;
    - files modified;
    - tests added;
    - commands executed;
    - test results;
    - architectural decisions;
    - trade-offs;
    - optional items not implemented.

Begin with Phase 1 and continue sequentially only while all previous acceptance criteria remain passing.

### Mandatory Stop Checkpoint

The real recruitment test has a 4-hour timebox. Phases 1 through 7 (Bootstrap, Domain and Ports, External Integration, Application Service, REST API, Cache, Error Handling) constitute the mandatory core that satisfies the original challenge requirements (consume the external API, expose a REST API, implement a caching layer).

Phases 8, 9, and 10 (i18n, CORS, Documentation and Polish) and the optional items in sections 20-21 (OpenAPI, Dockerfile) are stretch goals, not part of the mandatory core.

After Phase 7 passes all its acceptance criteria and `./mvnw clean test` succeeds:

1. Stop.
2. Do not start Phase 8.
3. Report:
   - files created and modified so far;
   - all tests added and their results;
   - confirmation that `./mvnw clean test` passes;
   - remaining time available, if known;
   - a short summary of what Phases 8-10 and the optional items would add.
4. Wait for explicit confirmation before continuing to Phase 8 or any later phase.

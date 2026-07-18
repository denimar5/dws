# Isobar FM API

A production-quality Java backend that consumes the external [Bands API](https://bands-api.vercel.app), exposes a REST API for multiple consumers, and implements an in-memory caching layer.

---

## Challenge Summary

Build a backend that:
- Fetches band data from an external provider
- Exposes a REST API with list, search, sort, and detail endpoints
- Caches the provider response to avoid repeated network calls
- Returns stable, typed error contracts
- Is testable, maintainable, and deployable

---

## Architecture

Pragmatic hexagonal (ports and adapters) architecture:

```
HTTP Request
    │
    ▼
┌─────────────────────────┐
│   Inbound Adapter       │  BandController (Spring MVC)
│   (adapter/in/web)      │  GlobalExceptionHandler
└────────────┬────────────┘
             │ uses
             ▼
┌─────────────────────────┐
│   Application Port In   │  FindBandsUseCase
│   (port/in)             │  GetBandDetailsUseCase
└────────────┬────────────┘
             │ implements
             ▼
┌─────────────────────────┐
│   Application Service   │  BandService
│   (application/service) │  search · sort · getById
└────────────┬────────────┘
             │ uses
             ▼
┌─────────────────────────┐
│   Application Port Out  │  LoadBandsPort
│   (port/out)            │
└────────────┬────────────┘
             │ implements
             ▼
┌─────────────────────────┐
│   Outbound Adapter      │  BandsApiAdapter (@Cacheable)
│   (adapter/out/bandsapi)│  BandsApiClient (HTTP Interface)
└────────────┬────────────┘
             │ calls
             ▼
    External Bands API
    https://bands-api.vercel.app/api/bands
```

**Dependency rule**: Domain and application layers have zero dependency on Spring MVC, RestClient, Jackson, Caffeine, or any infrastructure concern.

---

## Technology Choices

| Concern              | Technology                          |
|----------------------|-------------------------------------|
| Language             | Java 17                             |
| Framework            | Spring Boot 3.3.5                   |
| Build                | Maven Wrapper 3.9.6                 |
| HTTP Client          | Spring HTTP Interface + RestClient  |
| Cache                | Spring Cache + Caffeine             |
| Observability        | Spring Boot Actuator                |
| API Documentation    | Springdoc OpenAPI 2.6               |
| Testing              | JUnit 5 + Mockito + MockWebServer   |

---

## How to Run

```bash
./mvnw spring-boot:run
```

The API starts on port `8080`.

Override provider URL or timeouts via environment variables:

```bash
BANDS_API_BASE_URL=https://bands-api.vercel.app \
BANDS_API_CONNECT_TIMEOUT=2s \
BANDS_API_READ_TIMEOUT=5s \
BANDS_CACHE_TTL=10m \
./mvnw spring-boot:run
```

### Docker

```bash
docker build -t isobar-fm-api .
docker run --rm -p 8080:8080 isobar-fm-api
```

---

## How to Test

```bash
./mvnw clean test
```

Tests use MockWebServer — no real internet calls during the test suite.

---

## API Examples

### List all bands (default: sorted by name ascending)

```bash
curl "http://localhost:8080/api/v1/bands"
```

### Search by partial name

```bash
curl "http://localhost:8080/api/v1/bands?search=pink"
```

### Sort by popularity descending

```bash
curl "http://localhost:8080/api/v1/bands?sort=popularity&direction=desc"
```

### Combined search and sort

```bash
curl "http://localhost:8080/api/v1/bands?search=rock&sort=popularity&direction=desc"
```

### Get band details

```bash
curl "http://localhost:8080/api/v1/bands/00000000-0000-0000-0000-000000000000"
```

### Health check

```bash
curl "http://localhost:8080/actuator/health"
```

### Swagger UI

Open in browser: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### OpenAPI spec (JSON)

```bash
curl "http://localhost:8080/v3/api-docs"
```

---

## Cache Strategy

```
First request → external provider → map → cache entire list

Subsequent requests → cache hit → filter + sort in memory
```

- Backed by **Caffeine** (in-process, no Redis required)
- Cache key: `'all'` — a single entry for the entire band list
- `sync = true` prevents duplicate concurrent loads (thundering herd)
- Exceptions are **not** cached — a failed load retries on the next request
- TTL and maximum size are externalized:

```yaml
app:
  cache:
    bands:
      ttl: ${BANDS_CACHE_TTL:10m}
      maximum-size: ${BANDS_CACHE_MAXIMUM_SIZE:10}
```

---

## Error Handling Strategy

All errors return a stable JSON contract:

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

| Situation                  | HTTP | Code                            |
|----------------------------|-----:|---------------------------------|
| Band not found             |  404 | `BAND_NOT_FOUND`                |
| Malformed UUID             |  400 | `INVALID_REQUEST`               |
| Invalid sort value         |  400 | `INVALID_SORT`                  |
| Invalid direction value    |  400 | `INVALID_DIRECTION`             |
| Provider unavailable       |  503 | `BANDS_PROVIDER_UNAVAILABLE`    |
| Provider invalid response  |  502 | `BANDS_PROVIDER_INVALID_RESPONSE` |
| Unexpected error           |  500 | `INTERNAL_SERVER_ERROR`         |

Stack traces are never exposed in responses. Provider internals never leak through the API boundary.

---

## Configuration — Environment Variables

| Variable                    | Default                             | Description                        |
|-----------------------------|-------------------------------------|------------------------------------|
| `BANDS_API_BASE_URL`        | `https://bands-api.vercel.app`      | External provider base URL         |
| `BANDS_API_CONNECT_TIMEOUT` | `2s`                                | TCP connect timeout                |
| `BANDS_API_READ_TIMEOUT`    | `5s`                                | Response read timeout              |
| `BANDS_CACHE_TTL`           | `10m`                               | Cache time-to-live                 |
| `BANDS_CACHE_MAXIMUM_SIZE`  | `10`                                | Max cache entries                  |
| `CORS_ALLOWED_ORIGIN_1`     | `http://localhost:3000`             | Allowed CORS origin 1              |
| `CORS_ALLOWED_ORIGIN_2`     | `http://localhost:4200`             | Allowed CORS origin 2              |
| `CORS_ALLOWED_ORIGIN_3`     | `http://localhost:5173`             | Allowed CORS origin 3              |

---

## Trade-offs

- **In-memory cache only**: Caffeine provides low-latency caching without operational overhead. A distributed cache (Redis) would be needed if the application scales to multiple instances, but was explicitly out of scope.
- **Single cache entry for all bands**: Simplest strategy consistent with the provider's single-endpoint design. All filtering and sorting happen in application memory after one cached load.
- **No authentication**: Out of scope. The API is designed to be placed behind an API gateway or load balancer that handles auth if needed.
- **Album details not fetched**: No documented album-details endpoint exists on the provider. Only album IDs are returned.
- **Spring HTTP Interface over RestTemplate**: Cleaner declarative style, type-safe, integrates naturally with RestClient. RestTemplate is deprecated in Spring 6.

---

## Known Limitations

- The cache is per-instance. Horizontal scaling requires a shared cache (Redis) or cache invalidation strategy.
- No retry policy on provider failure. A circuit breaker (Resilience4j) would improve fault tolerance under sustained provider outages.
- i18n (Accept-Language) and CORS are stretch goals not yet implemented — error messages are in English only and CORS is not configured.

---

## AI-Assisted Development Disclosure

This project was developed using a spec-driven workflow with **Claude Code** as an implementation assistant. Architectural decisions, requirements, acceptance criteria, review, testing strategy, and final validation remained under developer ownership.

Claude Code was used to:
- scaffold code from explicit specifications
- accelerate repetitive implementation
- suggest test cases
- support refactoring
- review consistency across layers

All generated code was reviewed, tested, and adjusted before acceptance.

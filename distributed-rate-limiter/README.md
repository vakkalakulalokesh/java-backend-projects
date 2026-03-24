# Distributed Rate Limiter & API Gateway

A production-grade **distributed rate limiting** library with multiple algorithms, backed by **Redis** and **Lua** for atomic, multi-key operations, plus a **demo API Gateway** that shows how to compose filters, JPA route configuration, analytics, and declarative `@RateLimit` enforcement.

## Architecture

```
                    +------------------+
                    |  Route Config    |
                    |  (JPA / H2)      |
                    +--------+---------+
                             |
                             v
+--------+       +-----------+------------+       +---------------------+
| Client | ----> | API Gateway (Spring) | ----> | RateLimitingFilter  |
+--------+       +-----------+------------+       +----------+----------+
                             ^                                 |
                             |                                 v
                    +--------+---------+              +--------+---------+
                    | @RateLimit (AOP) |              | rate-limiter-core |
                    +------------------+              +--------+---------+
                                                               |
                                                               v
                                                      +--------+---------+
                                                      | Redis + Lua       |
                                                      | (atomic scripts)  |
                                                      +-------------------+
                             |
                             v
                    +------------------+
                    | AnalyticsService |
                    | (Redis hashes)   |
                    +------------------+
```

Traffic hits the gateway: servlet **filters** enforce per-route policies using the core library, while annotated controllers can opt into limits without duplicating filter logic. **Analytics** aggregates success/rejection counts in Redis for a lightweight operations view.

## Algorithm comparison

| Algorithm | Pros | Cons | Best for |
|-----------|------|------|----------|
| **Token bucket** | Smooth refill, tolerates bursts | Needs tuning of refill vs capacity | APIs with predictable average rate but bursty clients |
| **Sliding window log** | Precise per-window counting | Memory proportional to events (ZSET members) | Strict fairness, lower traffic endpoints |
| **Sliding window counter** | Cheap, fixed memory, good approximation | Approximate under fast bucket rotation | High QPS, large fleet of gateways |
| **Fixed window** | Extremely cheap (`INCR` + TTL) | Boundary spikes at window edges | Simple per-minute dashboards, coarse fairness |
| **Leaky bucket** | Shapes traffic to steady outflow | Less intuitive for “N per minute” product language | Downstream protection, queue-like pacing |

## Tech stack

- **Java 17+** (bytecode target 17; verified build includes newer JDK toolchains when paired with current Lombok)
- **Spring Boot 3.2.x**
- **Maven** multi-module build
- **Redis 7** + **Lua** scripts executed via `StringRedisTemplate`
- **H2** + **JPA** for route metadata in the demo
- **Springdoc OpenAPI** for interactive docs
- **Docker** & **docker-compose** for a portable demo environment

## Features

- Five **battle-tested** rate limiting strategies with Redis-backed state
- **Lua** scripts for **atomic** read–modify–write per logical key
- **Spring Boot auto-configuration** (`AutoConfiguration.imports`) in **starter** style
- **`@RateLimit` annotation** with **AOP**, optional **SpEL** keys, and standard `X-RateLimit-*` headers
- **Per-route** gateway limits loaded from the database with an in-memory **active-route cache**
- **Real-time analytics** (totals, allow/deny counts, average duration, top rejected routes)
- **Dockerfile** + **compose** wiring for Redis + app

## Getting started

### Prerequisites

- JDK **17+** and **Maven 3.9+**
- **Redis 7** (local install or Docker)
- (Optional) **Docker Compose** to run the full stack

### Run locally

```bash
# Terminal 1 – Redis
docker run --rm -p 6379:6379 redis:7-alpine

# Terminal 2 – Demo gateway
cd distributed-rate-limiter
mvn -pl api-gateway-demo spring-boot:run
```

### Run with Docker Compose

```bash
cd distributed-rate-limiter
docker compose up --build
```

The gateway listens on **http://localhost:8080**.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:gateway`, user `sa`, empty password)

## Testing the rate limiter

### Declarative demo endpoints

```bash
# Unlimited baseline
curl -s http://localhost:8080/api/v1/demo/public

# Token bucket (10 tokens / minute, burst-friendly refill)
for i in $(seq 1 15); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/demo/limited
done
```

Expect `200` responses until the bucket is drained, then `429` with `X-RateLimit-*` headers from the exception handler.

### Gateway filter (database-driven routes)

Sample data seeds three routes, including:

- `GET /api/v1/external/demo` — fixed window (**5** requests / **60s**)

```bash
for i in $(seq 1 8); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/external/demo
done
```

### Analytics

```bash
curl -s http://localhost:8080/api/v1/analytics/metrics | jq .
curl -s http://localhost:8080/api/v1/analytics/top-limited | jq .
```

## System design notes

### Why Redis + Lua?

Rate limiting is a **read–modify–write** problem. Without atomicity, concurrent requests see stale counters and over-admit traffic. Redis **Lua** executes atomically in the server thread: all logic for refill, eviction, and admission completes before another command interleaves.

### Race conditions

Classic `GET` + `SET` patterns lose updates under concurrency. The implementations in `rate-limiter-core` use **single-key (or key-per-window) Lua scripts** so limit checks and mutations are **one atomic unit**.

### Distributed vs in-memory

| | In-memory | Redis (this project) |
|---|-----------|----------------------|
| Consistency per instance | Easy | N/A |
| Consistency cluster-wide | Poor | **Strong** (single source of truth) |
| Latency | Lowest | Low (one RTT + script) |
| Ops complexity | Lowest | Moderate (Redis HA, persistence policies) |

### Horizontal scaling

Each gateway instance is **stateless** regarding limits: Redis holds shared counters. Scale **gateway pods** behind a load balancer; all instances enforce the same global budget per key.

### Clock synchronization

Algorithms using wall-clock millis (`System.currentTimeMillis()` passed into Lua) assume **reasonably synchronized** clocks across gateways. Large skew can shift windows slightly. Mitigations: NTP discipline on nodes, stickier “epoch provider” services, or Redis `TIME` if you centralize time reads (trade-offs: extra round trips).

## API documentation (curl)

### Gateway route CRUD

```bash
curl -s http://localhost:8080/api/v1/gateway/routes | jq .

curl -s -X POST http://localhost:8080/api/v1/gateway/routes \
  -H 'Content-Type: application/json' \
  -d '{
    "path": "/api/v1/external/custom",
    "targetUrl": "https://httpbin.org/json",
    "method": "GET",
    "rateLimitMaxRequests": 20,
    "rateLimitWindowMs": 60000,
    "rateLimitAlgorithm": "SLIDING_WINDOW_LOG",
    "active": true,
    "description": "Custom route"
  }'
```

### Client identification

- Optional `X-API-Key: <token>` header (preferred when present)
- Otherwise first hop in `X-Forwarded-For`, falling back to `remoteAddr`

## Configuration

### Library (`rate-limiter-core`)

```yaml
rate-limiter:
  enabled: true
  default-algorithm: SLIDING_WINDOW_COUNTER
  default-max-requests: 100
  default-window-ms: 60000
  rules:
    partner-tier:
      algorithm: TOKEN_BUCKET
      max-requests: 200
      window-ms: 60000
```

Disable entirely:

```yaml
rate-limiter:
  enabled: false
```

### Redis

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Docker Compose already injects `SPRING_DATA_REDIS_HOST=redis` for the `app` service.

## Modules

- **`rate-limiter-core`** — algorithms, factory, `@RateLimit`, AOP aspect, auto-configuration, properties
- **`api-gateway-demo`** — JPA routes, filters, analytics, OpenAPI, sample data

## Future enhancements

- Pluggable **keying strategies** (JWT subject, tenant header, GraphQL operation)
- **Redis Cluster** slot-aware key hashing and **heartbeat** metrics
- **Adaptive limits** based on error rates or CPU saturation
- **gRPC / WebFlux** adapters sharing the same Lua core
- **Formal verification** harness replaying production traffic traces

## License

Demonstration project for interviews and learning; add a license if you fork for public use.

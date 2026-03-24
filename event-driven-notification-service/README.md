# event-driven-notification-service

Production-style **Spring Boot 3.2** microservice that accepts notification requests over REST, persists state in **PostgreSQL**, publishes work to **Apache Kafka** with **priority-based topics**, and delivers messages through pluggable **channel handlers** (email, SMS, push, in-app WebSocket). **Redis** backs caching for reads and hot notification metadata; failed deliveries are retried and eventually routed to a **dead-letter topic** with DLQ status in the database.

---

## Architecture (ASCII)

```
                                    +------------------+
                                    |   PostgreSQL     |
                                    | (notifications,  |
                                    |   templates)     |
                                    +--------+---------+
                                             ^
                                             |
+----------+    REST     +-------------------+-----------+     produce      +----------------+
| Clients  +----------->|  Notification API   +------------------------------> Kafka cluster  |
| (HTTP)   |            |  Template API       |                              |                |
+----------+            +----------+----------+                              | high-priority  |
                                    |                                        | standard       |
                                    |                                        | bulk           |
                                    |                                        | dead-letter    |
                                    |                                        +-------+--------+
                                    |                                                |
                                    |                                                | consume
                                    |                                        +-------v--------+
                                    |                                        |  Consumers     |
                                    |                                        |  (listeners)   |
                                    |                                        +-------+--------+
                                    |                                                |
                                    |                                                v
                                    |                                        +---------------+
                                    |                                        | Dispatcher    |
                                    |                                        | (strategy)    |
                                    |                                        +---+---+---+---++
                                    |                                            |   |   |
                         cache     |            +-------------------------------+   |   |
                    +--------------+---+        |                                   |   |
                    | Redis (cache +        +----v----+   +--------+   +---------v---------+
                    |  recent payload)      | Email   |   |  SMS   |   | Push / In-app     |
                    +------------------+    handler |   | handler|   | (STOMP/WebSocket) |
                                              +---------+   +--------+   +-------------------+
```

---

## Tech stack

| Area | Technology |
|------|------------|
| Runtime | Java 17+ |
| Framework | Spring Boot 3.2.x, Spring Web, Data JPA, Data Redis |
| Messaging | Spring Kafka, Confluent-style ZooKeeper + Kafka (Docker) |
| API docs | springdoc-openapi 2.3.0 (Swagger UI) |
| Database | PostgreSQL 15 (H2 for tests) |
| Cache | Spring Cache + Redis (in-memory cache in `test` profile) |
| Real-time | STOMP over WebSocket (`/ws`, `/topic/...`) |
| Build | Maven, multi-stage Docker image |

---

## Features

- REST API for single and bulk notification submission with validation (Jakarta Validation).
- JPA entities with JSON metadata (`@JdbcTypeCode`), enums for channel, priority, and lifecycle status (including **DLQ**).
- Kafka topics: `notification.high-priority`, `notification.standard`, `notification.bulk`, `notification.dead-letter`.
- Consumer concurrency tuned per topic; manual acknowledgements; container-level `DefaultErrorHandler` with backoff (Spring Kafka 3 successor to `SeekToCurrentErrorHandler` for recoverable consumer errors).
- Strategy pattern: `NotificationDispatcher` selects `NotificationChannelHandler` by channel.
- Simulated channel delivery with latency and probabilistic failures for demos and resilience testing.
- Template engine with `{{placeholder}}` substitution and Redis-backed template resolution.
- Global exception handling with a consistent JSON error body.
- Actuator health endpoint for container orchestration.

---

## API documentation

With the app running, open **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

### Example `curl` calls

**Send a notification**

```bash
curl -sS -X POST http://localhost:8080/api/v1/notifications \
  -H 'Content-Type: application/json' \
  -d '{
    "recipientId": "user-42",
    "channel": "EMAIL",
    "priority": "HIGH",
    "subject": "Welcome",
    "content": "Thanks for signing up."
  }'
```

**Bulk send**

```bash
curl -sS -X POST http://localhost:8080/api/v1/notifications/bulk \
  -H 'Content-Type: application/json' \
  -d '{
    "notifications": [
      { "recipientId": "a", "channel": "SMS", "content": "Hello A" },
      { "recipientId": "b", "channel": "PUSH", "content": "Hello B" }
    ]
  }'
```

**Stats**

```bash
curl -sS http://localhost:8080/api/v1/notifications/stats
```

**Create template**

```bash
curl -sS -X POST http://localhost:8080/api/v1/templates \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "order-shipped",
    "channel": "EMAIL",
    "subject": "Order {{orderId}} shipped",
    "bodyTemplate": "Hi {{name}}, order {{orderId}} is on the way.",
    "active": true
  }'
```

**WebSocket (STOMP)**  
Connect a STOMP client to `http://localhost:8080/ws` (SockJS enabled) and subscribe to `/topic/notifications/{recipientId}` for in-app messages.

---

## How to run

```bash
cd event-driven-notification-service
docker compose up --build
```

- Application: [http://localhost:8080](http://localhost:8080)  
- Kafka from the host (advertised listener): `localhost:29092`  
- Postgres: `localhost:5432`, database `notification_db`, user/password `postgres` / `postgres`  
- Redis: `localhost:6379`

Local profile (without Docker service names) is available as `local` in `application.yml`.

---

## System design considerations

1. **Why Kafka**  
   Durable, ordered, replayable log with consumer groups supports back-pressure, horizontal scale-out of consumers, and clear separation between API ingestion and delivery workloads.

2. **Strategy pattern for channels**  
   `NotificationChannelHandler` implementations stay isolated; adding a channel means adding a component without changing dispatcher control flow beyond discovery of `supports(channel)`.

3. **Priority-based topic routing**  
   HIGH traffic is isolated on `notification.high-priority` with higher consumer concurrency so operational incidents or bulk mail cannot starve critical alerts.

4. **Dead-letter queue**  
   After bounded retries, messages go to `notification.dead-letter` while the DB reflects `DLQ` for analytics, reconciliation, and alerting.

5. **Redis caching**  
   Read-heavy paths (`getNotification`, templates) use Spring Cache; dispatcher additionally writes a short-TTL snapshot for recent deliveries. Profile `test` uses an in-memory `ConcurrentMapCacheManager` so CI does not require Redis.

6. **Horizontal scaling**  
   Stateless API instances; Kafka partitions bound throughput per topic; consumers in the same group share partition assignment; PostgreSQL holds authoritative state; Redis is an optional performance layer (safe to rebuild from DB).

---

## Future enhancements

- Schema registry (Avro/JSON Schema) for Kafka payloads and explicit version evolution.
- OAuth2 / JWT for APIs and WebSocket handshake security.
- Outbox pattern for exactly-once handoff from DB to Kafka.
- Dedicated retry topics with exponential backoff and jitter.
- Metrics (Micrometer) and distributed tracing (OpenTelemetry).
- Rate limiting and quota per tenant or recipient.

---

## Build & test

```bash
mvn clean verify
```

Uses **H2**, **Embedded Kafka**, and an in-memory cache manager under the `test` profile.

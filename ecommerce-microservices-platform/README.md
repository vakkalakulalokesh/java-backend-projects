# E-Commerce Microservices Platform

Full-scale microservices e-commerce backend with **Saga Orchestration** for distributed transaction management, **CQRS-style command/query split** in the order service, **event-driven integration** over Apache Kafka, **Redis-backed saga materialized views**, **idempotent payments**, and **optimistically locked inventory** with reservation expiry.

## Architecture (ASCII)

```
                         +------------------+
                         |   Apache Kafka   |
                         +--------+---------+
                                  |
    +-----------------------------+-----------------------------+
    |                             |                             |
    v                             v                             v
+-----------+              +------------+              +---------------+
|  Product  |              |   Order    |              |   Payment     |
|  Service  |              |  Service   |              |   Service     |
|  :8081    |              |  :8082     |              |   :8083       |
+----+------+              +------+-----+              +-------+-------+
     |                            |                            |
     | PostgreSQL                 | PostgreSQL                 | PostgreSQL
     v                            v                            v
 [product_db]                 [order_db]                 [payment_db]

+-----------+              +----------------+
| Inventory |              | Notification   |
| Service   |              | Service        |
| :8084     |              | :8085          |
+----+------+              +--------+-------+
     |                               |
     v                               v
[inventory_db]                 [notification_db]

        Redis <--- Order Service (saga fast path cache)
```

## Saga orchestration (happy path)

```
Order Service          Payment Service       Inventory Service      Notification Service
     |                       |                       |                       |
     |--orders.payment-req->|                       |                       |
     |                       |--process / persist-->|                       |
     |<-orders.payment-resp--|                       |                       |
     |                       |                       |                       |
     |--orders.inventory-req------------------------>|                       |
     |                       |                       |--reserve + confirm->|
     |<-orders.inventory-resp-----------------------|                       |
     |--orders.status-update (CONFIRMED)------------------------------------>|
```

## Saga compensation (failure path)

When **payment fails**, the orchestrator marks the saga failed, cancels the order path, and emits cancellation/status events (no refund because no `paymentId`).

When **inventory is insufficient** after a successful payment, the orchestrator enters **COMPENSATING**, calls the **payment refund API** (HTTP) with the stored `paymentId`, moves the order to **REFUNDED** (or **CANCELLED** when no payment to reverse), finalizes the saga as **FAILED**, and publishes **OrderCancelledEvent** / status updates for downstream consumers (including notifications).

## Microservices overview

| Service              | Port | Responsibility                         | Database        | Key patterns                          |
|----------------------|-----:|----------------------------------------|-----------------|---------------------------------------|
| product-service      | 8081 | Catalog, search, Redis cache           | `product_db`    | Cache-aside, JPA, OpenAPI             |
| order-service        | 8082 | Orders, **saga orchestration**        | `order_db`      | Saga, Kafka, Redis, CQRS split        |
| payment-service      | 8083 | Payments, idempotency, gateway sim     | `payment_db`    | Idempotency, Kafka consumer          |
| inventory-service    | 8084 | Stock, reservations, low-stock        | `inventory_db`  | Optimistic locking, scheduled cleanup   |
| notification-service | 8085 | Multi-channel notification log        | `notification_db` | Kafka fan-out, simulated channels   |

## Key design patterns

- **Saga Orchestration** — `SagaOrchestrator` in order-service owns state transitions, compensation order, and correlation by `orderId` / `sagaId`.
- **CQRS (order-service)** — `OrderCommandService` for writes (create, cancel), `OrderQueryService` for reads and stats; `OrderService` composes both.
- **Event-Driven Architecture** — Kafka topics connect payment and inventory reactions to orchestration and notifications.
- **Optimistic Locking** — `InventoryEntity.version` with retry on `ObjectOptimisticLockingFailureException`.
- **Idempotency** — `PaymentEntity.idempotencyKey` ensures replay-safe processing and consistent Kafka responses.
- **Database per Service** — Each bounded context has its own PostgreSQL database.
- **Reservation Expiry** — `ReservationCleanupService` runs every 60s to release expired `RESERVED` rows back to available stock.

## API documentation (OpenAPI)

- Product: `http://localhost:8081/swagger-ui.html`
- Order: `http://localhost:8082/swagger-ui.html`
- Payment: `http://localhost:8083/swagger-ui.html`
- Inventory: `http://localhost:8084/swagger-ui.html`

### curl examples

**Product — create**

```bash
curl -s -X POST http://localhost:8081/api/v1/products \
  -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-1","name":"Widget","description":"A widget","price":29.99,"category":"tools","brand":"Acme","imageUrl":"https://example.com/w.png"}'
```

**Inventory — restock**

```bash
curl -s -X POST http://localhost:8084/api/v1/inventory/restock \
  -H 'Content-Type: application/json' \
  -d '{"productId":"prod-1","quantity":100}'
```

**Order — place (starts saga)**

```bash
curl -s -X POST http://localhost:8082/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId":"cust-1",
    "items":[{"productId":"prod-1","productName":"Widget","quantity":2,"unitPrice":29.99}],
    "shippingAddress":{"street":"1 Main","city":"NYC","state":"NY","zipCode":"10001","country":"US"}
  }'
```

**Order — saga status**

```bash
curl -s http://localhost:8082/api/v1/orders/{orderId}/saga-status
```

**Payment — by order**

```bash
curl -s http://localhost:8083/api/v1/payments/order/{orderId}
```

**Notification — by order**

```bash
curl -s http://localhost:8085/api/v1/notifications/order/{orderId}
```

## Kafka topics

| Topic                      | Primary producer   | Primary consumer        | Payload / intent                    |
|----------------------------|--------------------|-------------------------|-------------------------------------|
| `orders.created`           | order-service      | (extension / analytics) | `OrderCreatedEvent`                 |
| `orders.payment-request`   | order-service      | payment-service         | Payment request envelope            |
| `orders.payment-response`  | payment-service    | order-service, notification | `PaymentProcessedEvent` / `PaymentFailedEvent` |
| `orders.inventory-request` | order-service      | inventory-service       | Lines to reserve                    |
| `orders.inventory-response`| inventory-service  | order-service           | `InventoryReservedEvent` / `InventoryInsufficientEvent` |
| `orders.status-update`     | order-service      | notification-service    | `OrderStatusUpdate`, completion/cancel events |

## How to run

### Prerequisites

- Java **17+** (build verified with JDK 17; Lombok **1.18.42** and compiler `annotationProcessorPaths` support newer JDKs including **25**).
- Maven **3.9+**
- Docker (optional, for full stack)

### Local (infrastructure only)

1. Start PostgreSQL, Redis, and Kafka (or use Docker Compose infra services only).
2. Create databases:

```bash
chmod +x ./init-db.sh
./init-db.sh
```

3. Build:

```bash
mvn clean verify
```

4. Run each service (separate terminals), e.g.:

```bash
java -jar product-service/target/product-service-1.0.0-SNAPSHOT.jar
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar
java -jar payment-service/target/payment-service-1.0.0-SNAPSHOT.jar
java -jar inventory-service/target/inventory-service-1.0.0-SNAPSHOT.jar
java -jar notification-service/target/notification-service-1.0.0-SNAPSHOT.jar
```

Defaults: Postgres `localhost:5432`, user/password `ecommerce`, Redis `localhost:6379`, Kafka `localhost:9092`. Order service calls payment at `http://localhost:8083` for saga refunds (`app.payment.base-url`).

### Docker Compose (full platform)

```bash
mvn -q clean package -DskipTests
docker compose up --build
```

Compose brings up Zookeeper, Kafka, PostgreSQL (with `init-databases.sql`), Redis, and all five Spring Boot services with wired `SPRING_*` overrides.

## System design discussion

- **Why Saga over 2PC** — 2PC blocks under partitions and couples availability to all participants; sagas model explicit forward steps and compensations, fitting microservices with separate databases.
- **Eventual consistency** — Order reads may lag briefly behind Kafka-driven side effects; orchestration state in PostgreSQL plus Redis cache is the source of truth for saga progression.
- **Idempotency** — Payment replay with the same `idempotencyKey` reproduces the same outcome and re-emits the Kafka response, protecting consumers that retry.
- **Optimistic vs pessimistic locking** — Inventory uses **versioned** rows and short retries: higher throughput under contention than long-held DB locks, with bounded retry cost.
- **Reservation expiry** — Holds stock during payment without permanent loss if the customer abandons the flow; the scheduler returns quantity to **available**.
- **Horizontal scaling** — Stateless services behind a load balancer; Kafka consumer groups partition load; databases shard per service; Redis can be clustered for saga cache.

## Future enhancements

- API Gateway with OAuth2/OIDC and rate limiting
- Service mesh (mTLS, traffic policies)
- Distributed tracing (OpenTelemetry) and structured correlation IDs
- Kubernetes Helm charts and GitOps
- GraphQL federation for read-optimized storefront aggregation
- Transactional outbox for guaranteed event publication

---

Stack: **Java 17+**, **Spring Boot 3.2.12**, **Maven** multi-module, **Lombok 1.18.42** (pinned in parent `dependencyManagement` with `maven-compiler-plugin` annotation processor path).

# Java Backend Projects

A collection of production-grade Java backend projects demonstrating enterprise-level system design, distributed systems, and event-driven architecture.

## Projects

### 1. [Event-Driven Notification Service](./event-driven-notification-service)
A production-grade Spring Boot microservice demonstrating event-driven architecture with Apache Kafka for asynchronous notification processing.

**Tech Stack:** Java 17, Spring Boot 3.2, Apache Kafka, Redis, PostgreSQL, WebSocket (STOMP), Docker

**Key Highlights:**
- Priority-based Kafka topic routing (high-priority, standard, bulk, dead-letter)
- Strategy Pattern for multi-channel dispatch (Email, SMS, Push, In-App WebSocket)
- Dead Letter Queue with retry mechanisms for fault tolerance
- Redis caching with Spring Cache abstraction
- Real-time in-app notifications via STOMP/WebSocket
- Swagger UI for API documentation
- Fully containerized with Docker Compose

### 2. [Distributed Rate Limiter & API Gateway](./distributed-rate-limiter)
A multi-module Maven project implementing a distributed rate limiting library with 5 algorithms, backed by Redis Lua scripts for atomic operations, plus a demo API Gateway.

**Tech Stack:** Java 17, Spring Boot 3.2, Redis, Lua Scripts, Spring AOP, H2, Docker

**Key Highlights:**
- 5 rate limiting algorithms: Token Bucket, Sliding Window Log, Sliding Window Counter, Fixed Window, Leaky Bucket
- Redis Lua scripts for atomic, race-condition-free enforcement
- Spring Boot Starter pattern with auto-configuration
- Custom `@RateLimit` annotation with AOP aspect
- Per-route database-driven rate limiting in API Gateway
- Real-time analytics dashboard for monitoring
- Comprehensive system design documentation

## Getting Started

Each project has its own detailed README with architecture diagrams, API documentation, and Docker Compose setup. Navigate to the project directory for specific instructions.

### Quick Start

```bash
# Run the Notification Service
cd event-driven-notification-service
docker compose up --build

# Run the Rate Limiter (in another terminal)
cd distributed-rate-limiter
docker compose up --build
```

## Author
**Vakkalakula Lokesh**
- Email: lokeshvakkalakula619@gmail.com
- LinkedIn: [Profile](https://linkedin.com)

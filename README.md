# Java Backend Projects

A collection of Java backend projects demonstrating enterprise-level system design, distributed systems, event-driven architecture, and microservices patterns.

## Projects

### 1. [Real-Time Fraud Detection Platform](./fraud-detection-platform)
A multi-service fraud detection and transaction monitoring platform for the fintech domain. Processes financial transactions in real-time through a configurable rule engine with 6 fraud detection rules.

**Architecture:** 3 Microservices + Shared Common Module

**Tech Stack:** Java 17, Spring Boot 3.2, Apache Kafka, Redis, PostgreSQL, WebSocket, Docker

**Key Highlights:**
- Real-time transaction scoring with Chain of Responsibility rule engine
- 6 configurable fraud rules: Amount Threshold, Velocity Check, Geo Anomaly, Blacklist, Time Pattern, Frequency Pattern
- Redis-backed velocity windows, geo tracking, and blacklist management
- Kafka-driven event pipeline: Transaction Ingestion → Fraud Scoring → Alert Generation
- Real-time fraud alerts via WebSocket (STOMP)
- Dashboard API with analytics and alert lifecycle management

---

### 2. [E-Commerce Microservices Platform](./ecommerce-microservices-platform) 
A full-scale e-commerce backend demonstrating Saga Orchestration for distributed transactions, CQRS, event sourcing, and microservices best practices.

**Architecture:** 5 Microservices + Shared Common Module

**Tech Stack:** Java 17, Spring Boot 3.2, Apache Kafka, Redis, PostgreSQL, Docker

**Key Highlights:**
- **Saga Orchestrator** for distributed transaction management (Order → Payment → Inventory)
- Compensating transactions for failure recovery (automatic refunds and stock release)
- Idempotent payment processing with simulated gateway
- Optimistic locking for inventory with reservation expiry pattern
- Event-driven inter-service communication via Kafka
- Product catalog with Redis caching and search
- Order state machine with full lifecycle tracking

---

### 3. [Event-Driven Notification Service](./event-driven-notification-service)
A notification microservice with Kafka for async processing and multi-channel delivery.

**Tech Stack:** Java 17, Spring Boot 3.2, Apache Kafka, Redis, PostgreSQL, WebSocket, Docker

**Key Highlights:**
- Priority-based Kafka topic routing (high-priority, standard, bulk, dead-letter)
- Strategy Pattern for multi-channel dispatch (Email, SMS, Push, In-App WebSocket)
- Dead Letter Queue with retry mechanisms
- Redis caching and template engine

---

### 4. [Distributed Rate Limiter & API Gateway](./distributed-rate-limiter)
A distributed rate limiting library with 5 algorithms backed by Redis Lua scripts, plus a demo API Gateway.

**Tech Stack:** Java 17, Spring Boot 3.2, Redis, Lua Scripts, Spring AOP, Docker

**Key Highlights:**
- 5 rate limiting algorithms: Token Bucket, Sliding Window Log/Counter, Fixed Window, Leaky Bucket
- Redis Lua scripts for atomic, race-condition-free enforcement
- Spring Boot Starter with custom `@RateLimit` annotation (AOP)
- Per-route gateway filtering and real-time analytics

---

## Architecture & Design Patterns Demonstrated

| Pattern | Project |
|---------|---------|
| Saga Orchestration | E-Commerce Platform |
| Event-Driven Architecture | All Projects |
| Chain of Responsibility (Rule Engine) | Fraud Detection |
| Strategy Pattern | Notification Service |
| CQRS | E-Commerce Platform |
| Optimistic Locking | E-Commerce (Inventory) |
| Idempotency | E-Commerce (Payment) |
| Circuit Breaker Ready | Fraud Detection, E-Commerce |
| Database per Service | Fraud Detection, E-Commerce |
| Dead Letter Queue | Notification Service, Fraud Detection |

## Quick Start

Each project includes Docker Compose for one-command setup:

```bash
# Fraud Detection Platform
cd fraud-detection-platform && docker compose up --build

# E-Commerce Platform
cd ecommerce-microservices-platform && docker compose up --build

# Notification Service
cd event-driven-notification-service && docker compose up --build

# Rate Limiter
cd distributed-rate-limiter && docker compose up --build
```

## Author
**Vakkalakula Lokesh** | [lokeshvakkalakula619@gmail.com](mailto:lokeshvakkalakula619@gmail.com)

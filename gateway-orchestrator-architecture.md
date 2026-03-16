# Gateway Saga Orchestrator Architecture

This document compiles the complete architectural plan, design decisions, specifications, and implementation tasks for evolving the E-commerce API Gateway into a Stateful Saga Orchestrator.

---

## 1. Proposal

### Why
The API Gateway currently acts as a dumb proxy — it forwards requests 1:1 to individual microservices via Spring Cloud Gateway routes. There is no orchestration logic: the frontend would need to call Cart, then Order, then Inventory, then Payment, then Delivery individually to complete a checkout. This defeats the purpose of having a central orchestrator and pushes complex, error-prone workflow logic to the client.

We need the Gateway to coordinate multi-step business flows (Saga Orchestrator pattern) across microservices, handling compensating actions on failure (e.g., releasing reserved stock if payment fails).

### What Changes
- **Add Saga Orchestrator logic** inside the Gateway to coordinate multi-service workflows (checkout, order cancellation, etc.)
- **Implement a Stateful Saga (State Machine)** persisting each step to a database (`saga_states`) to survive Gateway crashes
- **Add a Saga Recovery Job** to automatically resume or compensate stuck sagas after a system failure
- **Add reactive WebClient** to call individual microservices from within orchestration controllers
- **Keep existing Spring Cloud Gateway routes** for simple CRUD passthrough (e.g., `GET /api/v1/products`)
- **Add custom orchestration endpoints** that span multiple microservices (e.g., `POST /api/v1/checkout`)
- **Add centralized error handling** with compensating transactions (rollback on failure)
- **Add security layer** (JWT validation) at the Gateway level before routing or orchestrating

### Impact
- **Gateway microservice**: Major refactor — adds orchestration controllers, WebClient beans, security filters, error handling, **and a PostgreSQL Database with R2DBC for reactive state persistence**
- **`pom.xml`**: Needs `spring-boot-starter-webflux`, `spring-boot-starter-security`, `spring-boot-starter-data-r2dbc`, `postgresql`, `r2dbc-postgresql`, and `jjwt`
- **`application.yml`**: Needs DB connection strings and microservice base URLs
- **All 6 microservices**: No code changes required — they already expose REST APIs via their own controllers; the Gateway will call them via HTTP
- **API contracts**: New endpoints introduced (`/api/v1/checkout`, `/api/v1/orders/{id}/cancel`) that don't map to any single microservice

---

## 2. Technical Design

### Context
The e-commerce platform is composed of 6 independent microservices (Products, Inventory, Cart, Order, Payment, Delivery) each with their own database and REST API. The Gateway microservice currently uses Spring Cloud Gateway as a dumb proxy, forwarding requests 1:1 via path-based routing configured in `application.yml`.

The Gateway needs to evolve into a **Saga Orchestrator** — coordinating multi-step workflows across services (e.g., checkout: Cart → Inventory → Order → Payment → Delivery) while also acting as the centralized security and routing layer.

The Gateway already runs on Spring Cloud Gateway (WebFlux/Netty). All downstream microservices expose standard REST APIs via Spring Boot WebMVC controllers.

### Goals / Non-Goals

**Goals:**
- Implement a hybrid Gateway that keeps passive routing for simple CRUD and adds orchestration controllers for multi-service flows
- Implement the **Checkout Saga** (Cart → reserve Inventory → create Order → process Payment → schedule Delivery) with compensating transactions on failure
- Implement **Order Cancellation Saga** (cancel Order → release Inventory → refund Payment → cancel Delivery)
- Add centralized JWT-based authentication/authorization as a Gateway filter
- Configure reactive WebClient beans for calling downstream microservices

**Non-Goals:**
- Event-driven / message-broker-based Saga (Kafka, RabbitMQ) — we use synchronous HTTP orchestration for now
- Service Discovery (Eureka/Consul) — we use static URLs initially
- Rate limiting — deferred to a later change
- User management / registration microservice — out of scope

### Decisions

#### 1. Synchronous HTTP Orchestration over Event-Driven Saga
- **Decision:** Use synchronous WebClient calls from the Gateway to orchestrate Sagas
- **Rationale:** Simpler to implement, debug, and reason about. The team is familiar with REST. Event-driven sagas (Kafka/RabbitMQ) add infrastructure complexity that isn't justified at this stage

#### 2. WebClient (Reactive) over RestTemplate/Feign
- **Decision:** Use Spring WebFlux `WebClient` for downstream HTTP calls
- **Rationale:** The Gateway already runs on Netty (via Spring Cloud Gateway). WebClient is the native reactive HTTP client, avoiding thread-blocking issues that RestTemplate would introduce. Feign doesn't support reactive stacks

#### 3. Custom Orchestration Controllers alongside Gateway Routes
- **Decision:** Colocate orchestration `@RestController` classes inside the Gateway alongside the existing route-based proxy
- **Rationale:** Spring Cloud Gateway supports custom WebFlux endpoints. Orchestrated flows (checkout, cancellation) need custom Java logic — they can't be expressed as simple route forwarding. Simple CRUD operations keep their passive routes

#### 4. JWT Security via Global Gateway Filter
- **Decision:** Implement a `GlobalFilter` that validates JWT tokens on every request before routing or orchestration
- **Rationale:** Centralizing auth at the Gateway means microservices don't need to implement their own JWT validation. The filter runs before any route predicate, covering both proxied and orchestrated endpoints

#### 5. Persistent State Machine (Saga Log)
- **Decision:** Implement a State Machine that persists every step of the Saga to a PostgreSQL database (`saga_states` table) using R2DBC.
- **Rationale:** If the Gateway crashes mid-saga, any non-persisted state is lost, leading to zombie states (e.g., inventory stuck in reserved status). Persisting the state allows a scheduled job to recover and compensate stuck sagas automatically.

### Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Synchronous orchestration has higher latency (sequential HTTP calls) | Acceptable for MVP; can parallelize independent steps (e.g., Payment + Delivery scheduling) later |
| Gateway requires its own Database (PostgreSQL) | Adds infrastructure dependency, but eliminates the single point of failure for saga states |
| Partial failure during compensation (e.g., refund call also fails) | Log compensation failures and use the persistent Saga Log to retry compensation later |
| WebClient timeout/connection issues to downstream services | Configure timeouts and circuit breakers (Resilience4J) in a follow-up change |
| JWT secret management | Use environment variables initially; migrate to Vault/secrets manager later |

---

## 3. Specifications

### 3.1 Saga Orchestration Requirements

**Requirement: Checkout Saga Orchestration**  
The Gateway SHALL expose a `POST /api/v1/checkout` endpoint that orchestrates the full checkout flow across multiple microservices in sequence: Cart → Inventory → Order → Payment → Delivery.
- **Scenario: Successful checkout:** Gateway retrieves cart, reserves inventory, creates order, processes payment, schedules delivery.
- **Scenario: Checkout fails at inventory reservation:** Gateway aborts checkout, preventing Order/Payment/Delivery creation.
- **Scenario: Checkout fails at payment processing:** Gateway executes compensating transactions (cancel order, release inventory).
- **Scenario: Checkout with empty cart:** Gateway returns 400 error.

**Requirement: Order Cancellation Saga**  
The Gateway SHALL expose a `POST /api/v1/orders/{orderId}/cancel` endpoint orchestrating order cancellation with compensating actions.
- **Scenario: Successful order cancellation:** Gateway updates order to CANCELLED, releases inventory, requests refund, cancels delivery.
- **Scenario: Cancel an already shipped order:** Gateway rejects cancellation (409 Conflict).

**Requirement: Compensating Transaction Handling**  
The orchestrator SHALL implement compensating transactions that undo completed steps when a later step fails.
- **Scenario: Compensation executes in reverse order:** Steps N-1 through 1 are compensated in reverse.
- **Scenario: Compensation step fails:** Gateway logs failure and continues compensating remaining steps.

**Requirement: Saga State Persistence and Failover Recovery**  
The Gateway SHALL act as a Saga Execution Coordinator (SEC) by persisting the state of each saga step to a database.
- **Scenario: Saga step completion is persisted:** Gateway writes new saga state to `saga_states` table before next step.
- **Scenario: Recovery of interrupted sagas:** Background scheduled job detects stuck sagas, marks FAILED, and triggers compensation.

### 3.2 Gateway Security Requirements

**Requirement: JWT Authentication Filter**  
The Gateway SHALL implement a `GlobalFilter` to validate JWT tokens.
- **Scenario: Valid JWT token:** Extract user claims, attach to request attributes.
- **Scenario: Missing/Expired/Invalid token:** Return 401 Unauthorized.

**Requirement: Public Endpoint Allowlist**  
The Gateway SHALL allow configured endpoints without a JWT token.
- **Scenario:** Public routes (health, login) bypass JWT validation.
- **Scenario:** Allowlist is configurable via `application.yml`.

### 3.3 Gateway WebClient Requirements

**Requirement: WebClient Bean Configuration**  
The Gateway SHALL configure a reactive `WebClient` with base URLs loaded from properties.
- **Scenario:** WebClient resolves URLs from `spring.cloud.gateway.microservices.*`.
- **Scenario:** Calls use non-blocking I/O (Mono/Flux).

**Requirement: WebClient Timeout Configuration**  
The Gateway SHALL configure connection and read timeouts.
- **Scenario:** Timeout exceeds default (5s) -> step failure -> triggers compensation.
- **Scenario:** Timeouts configurable per microservice.

---

## 4. Implementation Checklist

### 1. Gateway Dependencies & Configuration
- [ ] 1.1 Add `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, and `org.postgresql:r2dbc-postgresql` dependencies to Gateway `pom.xml`
- [ ] 1.2 Add `spring-boot-starter-security` dependency to Gateway `pom.xml`
- [ ] 1.3 Add `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` dependencies to Gateway `pom.xml`
- [ ] 1.4 Add PostgreSQL database connection properties to `application.yml` (for R2DBC)
- [ ] 1.5 Add microservice base URL properties to `application.yml` under `app.services.*` (products, inventory, cart, order, payment, delivery)
- [ ] 1.6 Add WebClient timeout properties to `application.yml` (connection and read timeout per service, default 5s)
- [ ] 1.7 Add JWT secret key and public endpoint allowlist to `application.yml`

### 2. WebClient Configuration
- [ ] 2.1 Create `WebClientConfig` class with `@Configuration` defining a `WebClient.Builder` bean with default timeout settings
- [ ] 2.2 Create typed WebClient beans (or a factory) for each microservice using base URLs from config properties
- [ ] 2.3 Create DTO classes in Gateway for responses received from downstream microservices (reuse existing DTOs or create lightweight wrappers)

### 3. JWT Security Filter
- [ ] 3.1 Create `JwtUtil` utility class to parse/validate JWT tokens and extract claims (userId, roles)
- [ ] 3.2 Create `JwtAuthenticationFilter` implementing `GlobalFilter` that intercepts all requests
- [ ] 3.3 Implement public endpoint allowlist logic — skip JWT validation for configured paths (health check, login, product listing)
- [ ] 3.4 Implement token extraction from `Authorization: Bearer <token>` header
- [ ] 3.5 Implement token validation (signature, expiration) with proper 401 error responses
- [ ] 3.6 Attach user claims (userId, roles) to request attributes for downstream use
- [ ] 3.7 Create `SecurityConfig` class disabling default Spring Security CSRF/form login for the reactive Gateway

### 4. Checkout Saga Orchestration
- [ ] 4.1 Create `CheckoutController` with `POST /api/v1/checkout` endpoint accepting `userId` in the request body
- [ ] 4.2 Create `CheckoutService` (orchestrator) with the reactive saga pipeline
- [ ] 4.3 Implement Step 1: Call Cart service to get user's active cart via WebClient
- [ ] 4.4 Implement validation: return 400 if cart is empty
- [ ] 4.5 Implement Step 2: Call Inventory service to reserve stock for each cart item via WebClient
- [ ] 4.6 Implement Step 3: Call Order service to create order with cart items via WebClient
- [ ] 4.7 Implement Step 4: Call Payment service to process payment via WebClient
- [ ] 4.8 Implement Step 5: Call Delivery service to schedule delivery via WebClient
- [ ] 4.9 Create unified `CheckoutResponse` DTO returning orderId, status, and payment reference
- [ ] 4.10 Implement compensation: if Payment fails → cancel Order + release Inventory (reverse order)
- [ ] 4.11 Implement compensation: if Order creation fails → release Inventory
- [ ] 4.12 Add logging for each saga step (start, success, failure, compensation)

### 5. Order Cancellation Saga
- [ ] 5.1 Create `POST /api/v1/orders/{orderId}/cancel` endpoint in an `OrderCancellationController`
- [ ] 5.2 Create `OrderCancellationService` (orchestrator) with the cancellation saga pipeline
- [ ] 5.3 Implement Step 1: Call Order service to get order details and validate status (reject if SHIPPED/DELIVERED → 409)
- [ ] 5.4 Implement Step 2: Call Order service to update order status to CANCELLED
- [ ] 5.5 Implement Step 3: Call Inventory service to release reserved stock for each order item
- [ ] 5.6 Implement Step 4: Call Payment service to request refund (if payment was processed)
- [ ] 5.7 Implement Step 5: Call Delivery service to cancel scheduled delivery (if exists)
- [ ] 5.8 Add logging for cancellation saga steps and compensating failures

### 6. Error Handling & Observability
- [ ] 6.1 Create `GlobalExceptionHandler` with `@ControllerAdvice` for consistent error responses across orchestration endpoints
- [ ] 6.2 Create custom exception classes: `SagaStepFailedException`, `CompensationFailedException`, `InsufficientStockException`
- [ ] 6.3 Implement compensation failure logging with full context (saga ID, step name, error details)
- [ ] 6.4 Add structured logging (saga start, step transitions, saga completion/failure)

### 7. Saga Execution Coordinator (State Persistence)
- [ ] 7.1 Create Flyway/Liquibase migration script for `saga_states` table in Gateway database
- [ ] 7.2 Create `SagaState` entity class mapped to `saga_states` table
- [ ] 7.3 Create `SagaStateRepository` extending `R2dbcRepository`
- [ ] 7.4 Create `SagaExecutionCoordinator` service to wrap saga steps, persisting state transitions to the DB before/after each WebClient call
- [ ] 7.5 Refactor `CheckoutService` and `OrderCancellationService` to use `SagaExecutionCoordinator` for persisting state
- [ ] 7.6 Create `@Scheduled` Recovery Job to poll database for sagas stuck in non-terminal states (e.g., PENDING > 5 minutes)
- [ ] 7.7 Implement Recovery Job logic to trigger compensation for any stuck/failed sagas

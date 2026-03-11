## Context

The e-commerce platform is composed of 6 independent microservices (Products, Inventory, Cart, Order, Payment, Delivery) each with their own database and REST API. The Gateway microservice currently uses Spring Cloud Gateway as a dumb proxy, forwarding requests 1:1 via path-based routing configured in `application.yml`.

The Gateway needs to evolve into a **Saga Orchestrator** — coordinating multi-step workflows across services (e.g., checkout: Cart → Inventory → Order → Payment → Delivery) while also acting as the centralized security and routing layer.

The Gateway already runs on Spring Cloud Gateway (WebFlux/Netty). All downstream microservices expose standard REST APIs via Spring Boot WebMVC controllers.

## Goals / Non-Goals

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

## Decisions

### 1. Synchronous HTTP Orchestration over Event-Driven Saga
- **Decision:** Use synchronous WebClient calls from the Gateway to orchestrate Sagas
- **Rationale:** Simpler to implement, debug, and reason about. The team is familiar with REST. Event-driven sagas (Kafka/RabbitMQ) add infrastructure complexity that isn't justified at this stage
- **Alternative considered:** Choreography-based Saga using domain events — rejected because it distributes flow control across services, making failures harder to track and compensate

### 2. WebClient (Reactive) over RestTemplate/Feign
- **Decision:** Use Spring WebFlux `WebClient` for downstream HTTP calls
- **Rationale:** The Gateway already runs on Netty (via Spring Cloud Gateway). WebClient is the native reactive HTTP client, avoiding thread-blocking issues that RestTemplate would introduce. Feign doesn't support reactive stacks
- **Alternative considered:** OpenFeign — rejected because it's blocking and incompatible with the reactive Gateway runtime

### 3. Custom Orchestration Controllers alongside Gateway Routes
- **Decision:** Colocate orchestration `@RestController` classes inside the Gateway alongside the existing route-based proxy
- **Rationale:** Spring Cloud Gateway supports custom WebFlux endpoints. Orchestrated flows (checkout, cancellation) need custom Java logic — they can't be expressed as simple route forwarding. Simple CRUD operations keep their passive routes
- **Alternative considered:** Separate orchestrator microservice — rejected to avoid extra infrastructure; the Gateway is the natural place for this

### 4. JWT Security via Global Gateway Filter
- **Decision:** Implement a `GlobalFilter` that validates JWT tokens on every request before routing or orchestration
- **Rationale:** Centralizing auth at the Gateway means microservices don't need to implement their own JWT validation. The filter runs before any route predicate, covering both proxied and orchestrated endpoints
- **Alternative considered:** Spring Security's built-in OAuth2 Resource Server — could be used later, but a custom filter gives us more control initially

### 5. Compensating Transactions via Orchestrator Logic
- **Decision:** Implement compensation inline in the orchestration service (try/catch each step, undo on failure)
- **Rationale:** For the initial implementation, simple sequential compensation is sufficient. If a step fails, the orchestrator calls the compensating endpoint for each previously completed step in reverse order
- **Alternative considered:** Saga state machine (e.g., Spring Statemachine) — deferred as over-engineering for the current number of steps

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Synchronous orchestration has higher latency (sequential HTTP calls) | Acceptable for MVP; can parallelize independent steps (e.g., Payment + Delivery scheduling) later |
| Single point of failure at Gateway | Gateway is stateless — can be horizontally scaled behind a load balancer |
| Partial failure during compensation (e.g., refund call also fails) | Log compensation failures for manual intervention; add retry logic in a future iteration |
| WebClient timeout/connection issues to downstream services | Configure timeouts and circuit breakers (Resilience4J) in a follow-up change |
| JWT secret management | Use environment variables initially; migrate to Vault/secrets manager later |

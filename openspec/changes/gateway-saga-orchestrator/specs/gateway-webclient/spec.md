## ADDED Requirements

### Requirement: WebClient Bean Configuration
The Gateway SHALL configure a reactive `WebClient` bean with base URLs for each downstream microservice, loaded from `application.yml` properties.

#### Scenario: WebClient resolves microservice URLs from config
- **WHEN** the Gateway application starts
- **THEN** WebClient instances SHALL be available for each microservice (Products, Inventory, Cart, Order, Payment, Delivery) with their base URLs loaded from `spring.cloud.gateway.microservices.*` configuration properties

#### Scenario: WebClient uses non-blocking I/O
- **WHEN** the orchestrator calls a downstream microservice via WebClient
- **THEN** the call SHALL be non-blocking, returning a `Mono` or `Flux` and releasing the calling thread immediately

---

### Requirement: WebClient Timeout Configuration
The Gateway SHALL configure connection and read timeouts for all WebClient calls to downstream microservices.

#### Scenario: Downstream service does not respond within timeout
- **WHEN** a WebClient call exceeds the configured timeout (default 5 seconds)
- **THEN** the call SHALL fail with a timeout error and the orchestrator SHALL handle it as a step failure (triggering compensation if applicable)

#### Scenario: Timeouts are configurable per service
- **WHEN** the application starts
- **THEN** timeout values SHALL be configurable per microservice via `application.yml` properties

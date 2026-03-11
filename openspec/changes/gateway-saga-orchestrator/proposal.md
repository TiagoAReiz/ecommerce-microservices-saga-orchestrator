## Why

The API Gateway currently acts as a dumb proxy — it forwards requests 1:1 to individual microservices via Spring Cloud Gateway routes. There is no orchestration logic: the frontend would need to call Cart, then Order, then Inventory, then Payment, then Delivery individually to complete a checkout. This defeats the purpose of having a central orchestrator and pushes complex, error-prone workflow logic to the client.

We need the Gateway to coordinate multi-step business flows (Saga Orchestrator pattern) across microservices, handling compensating actions on failure (e.g., releasing reserved stock if payment fails).

## What Changes

- **Add Saga Orchestrator logic** inside the Gateway to coordinate multi-service workflows (checkout, order cancellation, etc.)
- **Add reactive WebClient** to call individual microservices from within orchestration controllers
- **Keep existing Spring Cloud Gateway routes** for simple CRUD passthrough (e.g., `GET /api/v1/products`)
- **Add custom orchestration endpoints** that span multiple microservices (e.g., `POST /api/v1/checkout`)
- **Add centralized error handling** with compensating transactions (rollback on failure)
- **Add security layer** (JWT validation) at the Gateway level before routing or orchestrating

## Capabilities

### New Capabilities
- `saga-orchestration`: Coordinates multi-service workflows (checkout flow, order cancellation) using the Saga Orchestrator pattern with compensating transactions
- `gateway-security`: Centralized JWT authentication/authorization filter applied at the Gateway before any request reaches a microservice
- `gateway-webclient`: Reactive HTTP client configuration (WebClient) for the Gateway to call downstream microservices during orchestration

### Modified Capabilities
_(No existing specs to modify)_

## Impact

- **Gateway microservice**: Major refactor — adds orchestration controllers, WebClient beans, security filters, and error handling alongside existing passive routes
- **`pom.xml`**: Needs `spring-boot-starter-webflux` (for WebClient), `spring-boot-starter-security`, and `jjwt` dependencies
- **`application.yml`**: Needs microservice base URLs extracted into config properties for WebClient usage
- **All 6 microservices**: No code changes required — they already expose REST APIs via their own controllers; the Gateway will call them via HTTP
- **API contracts**: New endpoints introduced (`/api/v1/checkout`, `/api/v1/orders/{id}/cancel`) that don't map to any single microservice

## 1. Gateway Dependencies & Configuration

- [ ] 1.1 Add `spring-boot-starter-webflux` dependency to Gateway `pom.xml` (for WebClient)
- [ ] 1.2 Add `spring-boot-starter-security` dependency to Gateway `pom.xml`
- [ ] 1.3 Add `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` dependencies to Gateway `pom.xml`
- [ ] 1.4 Add microservice base URL properties to `application.yml` under `app.services.*` (products, inventory, cart, order, payment, delivery)
- [ ] 1.5 Add WebClient timeout properties to `application.yml` (connection and read timeout per service, default 5s)
- [ ] 1.6 Add JWT secret key and public endpoint allowlist to `application.yml`

## 2. WebClient Configuration

- [ ] 2.1 Create `WebClientConfig` class with `@Configuration` defining a `WebClient.Builder` bean with default timeout settings
- [ ] 2.2 Create typed WebClient beans (or a factory) for each microservice using base URLs from config properties
- [ ] 2.3 Create DTO classes in Gateway for responses received from downstream microservices (reuse existing DTOs or create lightweight wrappers)

## 3. JWT Security Filter

- [ ] 3.1 Create `JwtUtil` utility class to parse/validate JWT tokens and extract claims (userId, roles)
- [ ] 3.2 Create `JwtAuthenticationFilter` implementing `GlobalFilter` that intercepts all requests
- [ ] 3.3 Implement public endpoint allowlist logic — skip JWT validation for configured paths (health check, login, product listing)
- [ ] 3.4 Implement token extraction from `Authorization: Bearer <token>` header
- [ ] 3.5 Implement token validation (signature, expiration) with proper 401 error responses
- [ ] 3.6 Attach user claims (userId, roles) to request attributes for downstream use
- [ ] 3.7 Create `SecurityConfig` class disabling default Spring Security CSRF/form login for the reactive Gateway

## 4. Checkout Saga Orchestration

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

## 5. Order Cancellation Saga

- [ ] 5.1 Create `POST /api/v1/orders/{orderId}/cancel` endpoint in an `OrderCancellationController`
- [ ] 5.2 Create `OrderCancellationService` (orchestrator) with the cancellation saga pipeline
- [ ] 5.3 Implement Step 1: Call Order service to get order details and validate status (reject if SHIPPED/DELIVERED → 409)
- [ ] 5.4 Implement Step 2: Call Order service to update order status to CANCELLED
- [ ] 5.5 Implement Step 3: Call Inventory service to release reserved stock for each order item
- [ ] 5.6 Implement Step 4: Call Payment service to request refund (if payment was processed)
- [ ] 5.7 Implement Step 5: Call Delivery service to cancel scheduled delivery (if exists)
- [ ] 5.8 Add logging for cancellation saga steps and compensating failures

## 6. Error Handling & Observability

- [ ] 6.1 Create `GlobalExceptionHandler` with `@ControllerAdvice` for consistent error responses across orchestration endpoints
- [ ] 6.2 Create custom exception classes: `SagaStepFailedException`, `CompensationFailedException`, `InsufficientStockException`
- [ ] 6.3 Implement compensation failure logging with full context (saga ID, step name, error details)
- [ ] 6.4 Add structured logging (saga start, step transitions, saga completion/failure)

## ADDED Requirements

### Requirement: JWT Authentication Filter
The Gateway SHALL implement a `GlobalFilter` that intercepts every incoming request and validates the JWT token in the `Authorization` header before allowing the request to proceed to routing or orchestration.

#### Scenario: Valid JWT token
- **WHEN** a request arrives with a valid, non-expired JWT in the `Authorization: Bearer <token>` header
- **THEN** the Gateway SHALL extract the user claims (userId, roles) from the token, attach them to the request attributes, and allow the request to proceed

#### Scenario: Missing Authorization header
- **WHEN** a request arrives without an `Authorization` header
- **THEN** the Gateway SHALL return a 401 Unauthorized response with an error message

#### Scenario: Expired JWT token
- **WHEN** a request arrives with an expired JWT token
- **THEN** the Gateway SHALL return a 401 Unauthorized response indicating the token has expired

#### Scenario: Invalid/malformed JWT token
- **WHEN** a request arrives with a JWT token that cannot be parsed or has an invalid signature
- **THEN** the Gateway SHALL return a 401 Unauthorized response indicating the token is invalid

---

### Requirement: Public Endpoint Allowlist
The Gateway SHALL allow certain endpoints to be accessed without a JWT token (public routes).

#### Scenario: Request to a public endpoint
- **WHEN** a request arrives at a public endpoint (e.g., health check, login, product listing)
- **THEN** the Gateway SHALL allow the request to proceed without JWT validation

#### Scenario: Public endpoints are configurable
- **WHEN** the application starts
- **THEN** the list of public endpoints SHALL be configurable via `application.yml` properties

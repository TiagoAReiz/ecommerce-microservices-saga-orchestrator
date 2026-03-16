## ADDED Requirements

### Requirement: Checkout Saga Orchestration
The Gateway SHALL expose a `POST /api/v1/checkout` endpoint that orchestrates the full checkout flow across multiple microservices in sequence: Cart → Inventory → Order → Payment → Delivery.

#### Scenario: Successful checkout
- **WHEN** a valid checkout request is received with a `userId`
- **THEN** the Gateway SHALL retrieve the user's active cart, reserve inventory for each cart item, create an order, process the payment, schedule the delivery, and return a unified checkout response with the order ID and status

#### Scenario: Checkout fails at inventory reservation
- **WHEN** a checkout is in progress and the Inventory service returns insufficient stock for any item
- **THEN** the Gateway SHALL abort the checkout, return an error response with details about which product has insufficient stock, and NOT proceed to Order/Payment/Delivery creation

#### Scenario: Checkout fails at payment processing
- **WHEN** a checkout is in progress and the Payment service returns a failure
- **THEN** the Gateway SHALL execute compensating transactions: cancel the created order (set status to CANCELLED) and release the reserved inventory for all items

#### Scenario: Checkout with empty cart
- **WHEN** a checkout request is received but the user's cart is empty
- **THEN** the Gateway SHALL return a 400 error indicating the cart has no items

---

### Requirement: Order Cancellation Saga
The Gateway SHALL expose a `POST /api/v1/orders/{orderId}/cancel` endpoint that orchestrates order cancellation with compensating actions across microservices.

#### Scenario: Successful order cancellation
- **WHEN** a cancellation request is received for an existing order with status CREATED or PAID
- **THEN** the Gateway SHALL update the order status to CANCELLED, release reserved inventory for all order items, request a payment refund if payment was processed, and cancel scheduled delivery if it exists

#### Scenario: Cancel an already shipped order
- **WHEN** a cancellation request is received for an order with status SHIPPED or DELIVERED
- **THEN** the Gateway SHALL reject the cancellation and return a 409 Conflict error indicating the order cannot be cancelled

---

### Requirement: Compensating Transaction Handling
The orchestrator SHALL implement compensating transactions that undo completed steps when a later step in the saga fails.

#### Scenario: Compensation executes in reverse order
- **WHEN** step N of a saga fails after steps 1 through N-1 have succeeded
- **THEN** the Gateway SHALL execute compensating actions for steps N-1 through 1 in reverse order

#### Scenario: Compensation step itself fails
- **WHEN** a compensating transaction fails (e.g., inventory release call times out)
- **THEN** the Gateway SHALL log the compensation failure with full context (saga ID, step, error) and continue compensating remaining steps

---

### Requirement: Saga State Persistence and Failover Recovery
The Gateway SHALL act as a Saga Execution Coordinator (SEC) by persisting the state of each saga step to a database to prevent stuck resources in the event of a Gateway crash.

#### Scenario: Saga step completion is persisted
- **WHEN** the orchestrator successfully completes a step of a saga (e.g., Cart retrieval, Inventory reservation)
- **THEN** the Gateway SHALL write the new saga state to the database (`saga_states` table) before proceeding to the next step

#### Scenario: Recovery of interrupted sagas
- **WHEN** a saga is interrupted due to a Gateway crash or restart (status remains in a non-terminal state like PENDING_INVENTORY for too long)
- **THEN** a background scheduled job SHALL detect the stuck saga, mark its status as FAILED, and trigger compensating transactions for any successfully completed steps to release downstream resources

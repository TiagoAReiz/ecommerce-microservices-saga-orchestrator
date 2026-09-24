package microservices.ecommerce.order.infrastructure.adapters.in.controllers;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.order.application.mappers.OrderMapper;
import microservices.ecommerce.order.application.ports.in.usecases.OrderUseCase;
import microservices.ecommerce.order.core.entities.Order;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderRequest;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderResponse;
import microservices.ecommerce.order.core.exceptions.ResourceNotFoundException;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.security.AccessDeniedException;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.security.Caller;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reads are restricted to the order's owner (or ADMIN); another user's order answers 404 so its existence
 * is not revealed. Creating orders and changing their status are back-office/saga operations.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderUseCase orderUseCase;
    private final OrderMapper orderMapper;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @Valid @RequestBody OrderRequest request) {
        Caller.from(userIdHeader, rolesHeader).requireAdminOrInternal();
        Order order = orderUseCase.createOrder(request);
        return new ResponseEntity<>(orderMapper.toResponse(order), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @PathVariable UUID id) {
        Order order = orderUseCase.getOrderById(id);
        if (!Caller.from(userIdHeader, rolesHeader).canAccess(order.getUserId())) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @PathVariable UUID userId) {
        if (!Caller.from(userIdHeader, rolesHeader).canAccess(userId)) {
            throw new AccessDeniedException("Orders of another user");
        }
        List<OrderResponse> responses = orderUseCase.getOrdersByUserId(userId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @PathVariable UUID id, @RequestParam String status) {
        Caller.from(userIdHeader, rolesHeader).requireAdminOrInternal();
        Order order = orderUseCase.updateOrderStatus(id, status);
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }
}

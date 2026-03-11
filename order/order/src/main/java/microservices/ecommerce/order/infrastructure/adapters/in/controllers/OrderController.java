package microservices.ecommerce.order.infrastructure.adapters.in.controllers;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.order.application.mappers.OrderMapper;
import microservices.ecommerce.order.application.ports.in.usecases.OrderUseCase;
import microservices.ecommerce.order.core.entities.Order;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderRequest;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderUseCase orderUseCase;
    private final OrderMapper orderMapper;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        Order order = orderUseCase.createOrder(request);
        return new ResponseEntity<>(orderMapper.toResponse(order), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        Order order = orderUseCase.getOrderById(id);
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable UUID userId) {
        List<OrderResponse> responses = orderUseCase.getOrdersByUserId(userId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable UUID id, @RequestParam String status) {
        Order order = orderUseCase.updateOrderStatus(id, status);
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }
}

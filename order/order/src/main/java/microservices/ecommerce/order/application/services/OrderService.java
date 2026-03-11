package microservices.ecommerce.order.application.services;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.order.application.ports.in.usecases.OrderUseCase;
import microservices.ecommerce.order.application.ports.out.repositories.OrderRepository;
import microservices.ecommerce.order.core.entities.Order;
import microservices.ecommerce.order.core.entities.OrderItem;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    public Order createOrder(OrderRequest orderRequest) {
        List<OrderItem> items = orderRequest.items().stream()
                .map(reqItem -> new OrderItem(
                        UUID.randomUUID(),
                        reqItem.productId(),
                        reqItem.quantity(),
                        BigDecimal.ZERO // Note: proper implementation would fetch current price via port/client
                ))
                .collect(Collectors.toList());

        BigDecimal totalAmount = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order(
                UUID.randomUUID(),
                orderRequest.userId(),
                "CREATED", // Initial status
                totalAmount,
                orderRequest.shippingAddressId(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                items);

        return orderRepository.save(order);
    }

    @Override
    public Order getOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    @Override
    public List<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public Order updateOrderStatus(UUID id, String status) {
        Order order = getOrderById(id);
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }
}

package microservices.ecommerce.order.application.ports.out.repositories;

import microservices.ecommerce.order.core.entities.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(UUID id);

    List<Order> findByUserId(UUID userId);
}

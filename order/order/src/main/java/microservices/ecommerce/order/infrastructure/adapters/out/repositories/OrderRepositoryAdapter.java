package microservices.ecommerce.order.infrastructure.adapters.out.repositories;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.order.application.mappers.OrderMapper;
import microservices.ecommerce.order.application.ports.out.repositories.OrderRepository;
import microservices.ecommerce.order.core.entities.Order;
import microservices.ecommerce.order.infrastructure.adapters.out.entities.OrderEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderMapper orderMapper;

    @Override
    public Order save(Order order) {
        OrderEntity entity = orderMapper.toEntity(order);
        // Ensure bidirectional relationship is set before saving
        if (entity != null && entity.getItems() != null) {
            entity.getItems().forEach(item -> item.setOrder(entity));
        }
        OrderEntity savedEntity = orderJpaRepository.save(entity);
        return orderMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return orderJpaRepository.findById(id).map(orderMapper::toDomain);
    }

    @Override
    public List<Order> findByUserId(UUID userId) {
        return orderJpaRepository.findByUserId(userId).stream()
                .map(orderMapper::toDomain)
                .collect(Collectors.toList());
    }
}

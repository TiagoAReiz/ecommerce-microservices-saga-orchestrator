package microservices.ecommerce.order.application.mappers;

import microservices.ecommerce.order.core.entities.Order;
import microservices.ecommerce.order.core.entities.OrderItem;
import microservices.ecommerce.order.infrastructure.adapters.out.entities.OrderEntity;
import microservices.ecommerce.order.infrastructure.adapters.out.entities.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderEntity toEntity(Order domain) {
        if (domain == null)
            return null;
        OrderEntity entity = OrderEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .status(domain.getStatus())
                .totalAmount(domain.getTotalAmount())
                .shippingAddressId(domain.getShippingAddressId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();

        if (domain.getItems() != null) {
            List<OrderItemEntity> itemEntities = domain.getItems().stream()
                    .map(itemDomain -> OrderItemEntity.builder()
                            .id(itemDomain.getId())
                            .productId(itemDomain.getProductId())
                            .quantity(itemDomain.getQuantity())
                            .unitPrice(itemDomain.getUnitPrice())
                            .order(entity)
                            .build())
                    .collect(Collectors.toList());
            entity.setItems(itemEntities);
        } else {
            entity.setItems(new ArrayList<>());
        }
        return entity;
    }

    public Order toDomain(OrderEntity entity) {
        if (entity == null)
            return null;

        List<OrderItem> itemDomains = new ArrayList<>();
        if (entity.getItems() != null) {
            itemDomains = entity.getItems().stream()
                    .map(itemEntity -> new OrderItem(
                            itemEntity.getId(),
                            itemEntity.getProductId(),
                            itemEntity.getQuantity(),
                            itemEntity.getUnitPrice()))
                    .collect(Collectors.toList());
        }

        return new Order(
                entity.getId(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getShippingAddressId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                itemDomains);
    }
}

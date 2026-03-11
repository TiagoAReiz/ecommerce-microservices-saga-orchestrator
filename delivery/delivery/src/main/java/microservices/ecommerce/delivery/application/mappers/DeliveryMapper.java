package microservices.ecommerce.delivery.application.mappers;

import microservices.ecommerce.delivery.core.entities.Delivery;
import microservices.ecommerce.delivery.infrastructure.adapters.out.entities.DeliveryEntity;
import org.springframework.stereotype.Component;

@Component
public class DeliveryMapper {

    public DeliveryEntity toEntity(Delivery domain) {
        if (domain == null)
            return null;
        return DeliveryEntity.builder()
                .id(domain.getId())
                .orderId(domain.getOrderId())
                .carrier(domain.getCarrier())
                .trackingCode(domain.getTrackingCode())
                .status(domain.getStatus())
                .estimatedDeliveryDate(domain.getEstimatedDeliveryDate())
                .actualDeliveryDate(domain.getActualDeliveryDate())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Delivery toDomain(DeliveryEntity entity) {
        if (entity == null)
            return null;
        return new Delivery(
                entity.getId(),
                entity.getOrderId(),
                entity.getCarrier(),
                entity.getTrackingCode(),
                entity.getStatus(),
                entity.getEstimatedDeliveryDate(),
                entity.getActualDeliveryDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryResponse toResponse(
            Delivery domain) {
        if (domain == null)
            return null;
        return new microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryResponse(
                domain.getId(),
                domain.getOrderId(),
                domain.getCarrier(),
                domain.getTrackingCode(),
                domain.getStatus(),
                domain.getEstimatedDeliveryDate(),
                domain.getActualDeliveryDate(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}

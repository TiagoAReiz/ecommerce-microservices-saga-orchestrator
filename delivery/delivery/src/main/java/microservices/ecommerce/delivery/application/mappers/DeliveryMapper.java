package microservices.ecommerce.delivery.application.mappers;

import microservices.ecommerce.delivery.core.entities.Delivery;
import microservices.ecommerce.delivery.infrastructure.adapters.out.entities.DeliveryEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
                .estimatedDeliveryDate(toDate(domain.getEstimatedDeliveryDate()))
                .actualDeliveryDate(toDate(domain.getActualDeliveryDate()))
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
                toDateTime(entity.getEstimatedDeliveryDate()),
                toDateTime(entity.getActualDeliveryDate()),
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

    // Delivery dates are persisted as DATE columns; the domain/API work with LocalDateTime.
    private static LocalDate toDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    private static LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
}

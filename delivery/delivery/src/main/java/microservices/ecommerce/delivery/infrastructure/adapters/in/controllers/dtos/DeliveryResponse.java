package microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID orderId,
        String carrier,
        String trackingCode,
        String status,
        LocalDateTime estimatedDeliveryDate,
        LocalDateTime actualDeliveryDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

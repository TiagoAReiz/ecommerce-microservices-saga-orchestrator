package microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryRequest(
        UUID orderId,
        String carrier,
        String trackingCode,
        LocalDateTime estimatedDeliveryDate) {
}

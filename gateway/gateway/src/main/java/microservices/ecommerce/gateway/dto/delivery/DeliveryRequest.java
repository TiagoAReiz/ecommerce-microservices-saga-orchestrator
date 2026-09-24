package microservices.ecommerce.gateway.dto.delivery;

import java.time.LocalDateTime;
import java.util.UUID;

/** @param userId owner of the delivery, so the delivery service can restrict reads to that user */
public record DeliveryRequest(
        UUID orderId,
        String carrier,
        String trackingCode,
        LocalDateTime estimatedDeliveryDate,
        UUID userId
) {}

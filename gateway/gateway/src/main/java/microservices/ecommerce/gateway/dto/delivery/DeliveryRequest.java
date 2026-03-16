package microservices.ecommerce.gateway.dto.delivery;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryRequest(
        UUID orderId,
        String carrier,
        String trackingCode,
        LocalDateTime estimatedDeliveryDate
) {}

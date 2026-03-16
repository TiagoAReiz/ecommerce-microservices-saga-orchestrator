package microservices.ecommerce.gateway.dto.cancellation;

import java.util.UUID;

public record CancellationResponse(
        UUID orderId,
        String orderStatus,
        String message
) {}

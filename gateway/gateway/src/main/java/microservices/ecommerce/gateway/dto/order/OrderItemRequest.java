package microservices.ecommerce.gateway.dto.order;

import java.util.UUID;

public record OrderItemRequest(
        UUID productId,
        int quantity
) {}

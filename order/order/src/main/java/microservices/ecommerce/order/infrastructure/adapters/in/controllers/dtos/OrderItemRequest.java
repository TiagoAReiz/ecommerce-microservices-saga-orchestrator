package microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos;

import java.util.UUID;

public record OrderItemRequest(
        UUID productId,
        int quantity) {
}

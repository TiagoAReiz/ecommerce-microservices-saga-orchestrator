package microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID userId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CartItemResponse> items) {
}

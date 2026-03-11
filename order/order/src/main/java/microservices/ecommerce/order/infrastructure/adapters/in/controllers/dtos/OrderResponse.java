package microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        String status,
        BigDecimal totalAmount,
        UUID shippingAddressId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items) {
}

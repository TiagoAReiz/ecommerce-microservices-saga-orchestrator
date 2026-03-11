package microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        int quantity,
        BigDecimal unitPrice) {
}

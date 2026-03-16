package microservices.ecommerce.gateway.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        int quantity,
        BigDecimal unitPrice
) {}

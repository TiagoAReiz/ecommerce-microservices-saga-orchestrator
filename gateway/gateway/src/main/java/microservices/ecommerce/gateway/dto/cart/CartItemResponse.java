package microservices.ecommerce.gateway.dto.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        int quantity,
        BigDecimal priceAtAddition
) {}

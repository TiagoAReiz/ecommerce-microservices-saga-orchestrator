package microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        int quantity,
        BigDecimal priceAtAddition) {
}

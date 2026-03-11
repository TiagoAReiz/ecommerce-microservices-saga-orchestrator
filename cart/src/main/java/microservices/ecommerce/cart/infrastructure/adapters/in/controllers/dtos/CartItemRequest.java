package microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemRequest(
        UUID productId,
        int quantity,
        BigDecimal priceAtAddition) {
}

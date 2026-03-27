package microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemRequest(
        @NotNull UUID productId,
        @Positive int quantity,
        @NotNull @Positive BigDecimal priceAtAddition) {
}

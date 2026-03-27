package microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull UUID productId,
        @Positive int quantity) {
}

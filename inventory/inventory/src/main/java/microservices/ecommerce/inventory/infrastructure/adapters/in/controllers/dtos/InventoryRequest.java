package microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record InventoryRequest(
        @NotNull UUID productId,
        @PositiveOrZero int quantityAvailable,
        @PositiveOrZero int quantityReserved) {
}

package microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.dtos;

import java.util.UUID;

public record InventoryRequest(
        UUID productId,
        int quantityAvailable,
        int quantityReserved) {
}

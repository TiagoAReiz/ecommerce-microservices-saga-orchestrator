package microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        int quantityAvailable,
        int quantityReserved,
        LocalDateTime updatedAt) {
}

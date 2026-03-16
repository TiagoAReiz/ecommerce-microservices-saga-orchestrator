package microservices.ecommerce.gateway.dto.inventory;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        int quantityAvailable,
        int quantityReserved,
        LocalDateTime updatedAt
) {}

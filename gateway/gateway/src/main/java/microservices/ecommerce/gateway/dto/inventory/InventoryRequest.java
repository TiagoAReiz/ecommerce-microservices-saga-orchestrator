package microservices.ecommerce.gateway.dto.inventory;

import java.util.UUID;

public record InventoryRequest(
        UUID productId,
        int quantityAvailable,
        int quantityReserved
) {}

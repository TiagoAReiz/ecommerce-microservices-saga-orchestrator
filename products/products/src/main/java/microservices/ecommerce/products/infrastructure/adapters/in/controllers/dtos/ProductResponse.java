package microservices.ecommerce.products.infrastructure.adapters.in.controllers.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String sku,
        UUID categoryId,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

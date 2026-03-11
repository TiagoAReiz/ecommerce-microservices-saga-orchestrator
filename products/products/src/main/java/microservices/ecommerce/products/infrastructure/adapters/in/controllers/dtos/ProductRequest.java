package microservices.ecommerce.products.infrastructure.adapters.in.controllers.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        String name,
        String description,
        BigDecimal price,
        String sku,
        UUID categoryId,
        boolean active) {
}

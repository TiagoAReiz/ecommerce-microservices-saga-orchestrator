package microservices.ecommerce.inventory.application.mappers;

import microservices.ecommerce.inventory.core.entities.Inventory;
import microservices.ecommerce.inventory.infrastructure.adapters.out.entities.InventoryEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryEntity toEntity(Inventory domain) {
        if (domain == null)
            return null;
        return InventoryEntity.builder()
                .id(domain.getId())
                .productId(domain.getProductId())
                .quantityAvailable(domain.getQuantityAvailable())
                .quantityReserved(domain.getQuantityReserved())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Inventory toDomain(InventoryEntity entity) {
        if (entity == null)
            return null;
        return new Inventory(
                entity.getId(),
                entity.getProductId(),
                entity.getQuantityAvailable(),
                entity.getQuantityReserved(),
                entity.getUpdatedAt());
    }

    public Inventory toDomain(
            microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.dtos.InventoryRequest request) {
        if (request == null)
            return null;
        return new Inventory(
                java.util.UUID.randomUUID(),
                request.productId(),
                request.quantityAvailable(),
                request.quantityReserved(),
                java.time.LocalDateTime.now());
    }

    public microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.dtos.InventoryResponse toResponse(
            Inventory domain) {
        if (domain == null)
            return null;
        return new microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.dtos.InventoryResponse(
                domain.getId(),
                domain.getProductId(),
                domain.getQuantityAvailable(),
                domain.getQuantityReserved(),
                domain.getUpdatedAt());
    }
}

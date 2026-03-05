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
}

package microservices.ecommerce.inventory.infrastructure.adapters.out.repositories;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.inventory.application.mappers.InventoryMapper;
import microservices.ecommerce.inventory.application.ports.out.repositories.InventoryRepository;
import microservices.ecommerce.inventory.core.entities.Inventory;
import microservices.ecommerce.inventory.infrastructure.adapters.out.entities.InventoryEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryRepositoryAdapter implements InventoryRepository {

    private final InventoryJpaRepository inventoryJpaRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    public Inventory save(Inventory inventory) {
        InventoryEntity entity = inventoryMapper.toEntity(inventory);
        InventoryEntity savedEntity = inventoryJpaRepository.save(entity);
        return inventoryMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Inventory> findByProductId(UUID productId) {
        return inventoryJpaRepository.findByProductId(productId).map(inventoryMapper::toDomain);
    }

    @Override
    public Optional<Inventory> findById(UUID id) {
        return inventoryJpaRepository.findById(id).map(inventoryMapper::toDomain);
    }
}

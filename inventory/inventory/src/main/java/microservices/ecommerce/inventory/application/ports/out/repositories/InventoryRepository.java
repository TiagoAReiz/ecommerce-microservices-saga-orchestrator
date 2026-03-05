package microservices.ecommerce.inventory.application.ports.out.repositories;

import microservices.ecommerce.inventory.core.entities.Inventory;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {
    Inventory save(Inventory inventory);

    Optional<Inventory> findByProductId(UUID productId);

    Optional<Inventory> findById(UUID id);
}

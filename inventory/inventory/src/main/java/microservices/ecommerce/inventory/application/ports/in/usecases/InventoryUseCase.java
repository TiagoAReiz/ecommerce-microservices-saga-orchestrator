package microservices.ecommerce.inventory.application.ports.in.usecases;

import microservices.ecommerce.inventory.core.entities.Inventory;
import java.util.UUID;

public interface InventoryUseCase {

    Inventory addStock(UUID productId, int quantity);

    Inventory reserveStock(UUID productId, int quantity);

    Inventory releaseStock(UUID productId, int quantity);

    Inventory getInventoryByProductId(UUID productId);
}

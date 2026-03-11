package microservices.ecommerce.inventory.application.services;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.inventory.application.ports.in.usecases.InventoryUseCase;
import microservices.ecommerce.inventory.application.ports.out.repositories.InventoryRepository;
import microservices.ecommerce.inventory.core.entities.Inventory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService implements InventoryUseCase {

    private final InventoryRepository inventoryRepository;

    @Override
    public Inventory addStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElse(new Inventory(UUID.randomUUID(), productId, 0, 0, LocalDateTime.now()));

        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);
        return inventoryRepository.save(inventory);
    }

    @Override
    public Inventory reserveStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));

        if (inventory.getQuantityAvailable() < quantity) {
            throw new RuntimeException("Insufficient stock available for product: " + productId);
        }

        inventory.setQuantityAvailable(inventory.getQuantityAvailable() - quantity);
        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);

        return inventoryRepository.save(inventory);
    }

    @Override
    public Inventory releaseStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));

        if (inventory.getQuantityReserved() < quantity) {
            throw new RuntimeException("Cannot release more stock than reserved for product: " + productId);
        }

        inventory.setQuantityReserved(inventory.getQuantityReserved() - quantity);
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);

        return inventoryRepository.save(inventory);
    }

    @Override
    public Inventory getInventoryByProductId(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseGet(() -> new Inventory(UUID.randomUUID(), productId, 0, 0, LocalDateTime.now())); // Or return
                                                                                                          // optional/throw
                                                                                                          // depending
                                                                                                          // on design
    }
}

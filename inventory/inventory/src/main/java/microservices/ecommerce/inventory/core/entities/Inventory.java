package microservices.ecommerce.inventory.core.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public class Inventory {

    private UUID id;
    private UUID productId;
    private int quantityAvailable;
    private int quantityReserved;
    private LocalDateTime updatedAt;

    public Inventory() {
    }

    public Inventory(UUID id, UUID productId, int quantityAvailable, int quantityReserved, LocalDateTime updatedAt) {
        this.id = id;
        this.productId = productId;
        this.quantityAvailable = quantityAvailable;
        this.quantityReserved = quantityReserved;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

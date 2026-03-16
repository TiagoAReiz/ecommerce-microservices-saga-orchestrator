package microservices.ecommerce.gateway.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    private final UUID productId;

    public InsufficientStockException(UUID productId, String message) {
        super(String.format("Insufficient stock for product [%s]: %s", productId, message));
        this.productId = productId;
    }

    public UUID getProductId() {
        return productId;
    }
}

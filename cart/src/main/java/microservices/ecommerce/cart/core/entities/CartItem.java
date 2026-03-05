package microservices.ecommerce.cart.core.entities;

import java.math.BigDecimal;
import java.util.UUID;

public class CartItem {

    private UUID id;
    private UUID productId;
    private int quantity;
    private BigDecimal priceAtAddition;

    public CartItem() {
    }

    public CartItem(UUID id, UUID productId, int quantity, BigDecimal priceAtAddition) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtAddition = priceAtAddition;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtAddition() {
        return priceAtAddition;
    }

    public void setPriceAtAddition(BigDecimal priceAtAddition) {
        this.priceAtAddition = priceAtAddition;
    }
}

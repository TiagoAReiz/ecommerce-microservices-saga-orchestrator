package microservices.ecommerce.cart.application.ports.in.usecases;

import microservices.ecommerce.cart.core.entities.Cart;
import java.util.UUID;
import microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos.CartItemRequest;

public interface CartUseCase {

    Cart addItemToCart(UUID userId, CartItemRequest itemRequest);

    Cart removeItemFromCart(UUID userId, UUID productId);

    Cart getCartByUserId(UUID userId);

    void clearCart(UUID userId);

    // Checkout could return something else later, but for now we'll keep it simple
    void checkoutCart(UUID userId);
}

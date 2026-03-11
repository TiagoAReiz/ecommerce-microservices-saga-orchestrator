package microservices.ecommerce.cart.application.services;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.cart.application.ports.in.usecases.CartUseCase;
import microservices.ecommerce.cart.application.ports.out.repositories.CartRepository;
import microservices.ecommerce.cart.core.entities.Cart;
import microservices.ecommerce.cart.core.entities.CartItem;
import microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos.CartItemRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService implements CartUseCase {

    private final CartRepository cartRepository;

    @Override
    public Cart addItemToCart(UUID userId, CartItemRequest itemRequest) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElse(new Cart(UUID.randomUUID(), userId, "ACTIVE", LocalDateTime.now(), LocalDateTime.now(),
                        new ArrayList<>()));

        List<CartItem> items = cart.getItems();
        boolean itemExists = false;

        for (CartItem item : items) {
            if (item.getProductId().equals(itemRequest.productId())) {
                item.setQuantity(item.getQuantity() + itemRequest.quantity());
                itemExists = true;
                break;
            }
        }

        if (!itemExists) {
            items.add(new CartItem(
                    UUID.randomUUID(),
                    itemRequest.productId(),
                    itemRequest.quantity(),
                    itemRequest.priceAtAddition()));
        }

        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeItemFromCart(UUID userId, UUID productId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new RuntimeException("Active cart not found for user: " + userId));

        List<CartItem> updatedItems = cart.getItems().stream()
                .filter(item -> !item.getProductId().equals(productId))
                .collect(Collectors.toList());

        cart.setItems(updatedItems);
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    @Override
    public Cart getCartByUserId(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElse(new Cart(UUID.randomUUID(), userId, "ACTIVE", LocalDateTime.now(), LocalDateTime.now(),
                        new ArrayList<>()));
    }

    @Override
    public void clearCart(UUID userId) {
        cartRepository.findByUserIdAndStatus(userId, "ACTIVE").ifPresent(cart -> {
            cart.getItems().clear();
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);
        });
    }

    @Override
    public void checkoutCart(UUID userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new RuntimeException("Active cart not found for user: " + userId));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot checkout an empty cart");
        }

        // Logic to transition status (could publish an event here for the Saga)
        cart.setStatus("COMPLETED");
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }
}

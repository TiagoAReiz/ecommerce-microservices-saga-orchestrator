package microservices.ecommerce.cart.infrastructure.adapters.in.controllers;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.cart.application.mappers.CartMapper;
import microservices.ecommerce.cart.application.ports.in.usecases.CartUseCase;
import microservices.ecommerce.cart.core.entities.Cart;
import microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos.CartItemRequest;
import microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos.CartResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartUseCase cartUseCase;
    private final CartMapper cartMapper;

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponse> addItemToCart(@PathVariable UUID userId, @Valid @RequestBody CartItemRequest request) {
        Cart cart = cartUseCase.addItemToCart(userId, request);
        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartResponse> removeItemFromCart(@PathVariable UUID userId, @PathVariable UUID productId) {
        Cart cart = cartUseCase.removeItemFromCart(userId, productId);
        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCartByUserId(@PathVariable UUID userId) {
        Cart cart = cartUseCase.getCartByUserId(userId);
        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable UUID userId) {
        cartUseCase.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<Void> checkoutCart(@PathVariable UUID userId) {
        cartUseCase.checkoutCart(userId);
        return ResponseEntity.ok().build();
    }
}

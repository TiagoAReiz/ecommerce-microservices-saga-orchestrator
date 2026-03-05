package microservices.ecommerce.cart.application.ports.out.repositories;

import microservices.ecommerce.cart.core.entities.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository {
    Cart save(Cart cart);

    Optional<Cart> findById(UUID id);

    Optional<Cart> findByUserIdAndStatus(UUID userId, String status);

    void deleteById(UUID id);
}

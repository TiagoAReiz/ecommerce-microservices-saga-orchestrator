package microservices.ecommerce.cart.infrastructure.adapters.out.repositories;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.cart.application.mappers.CartMapper;
import microservices.ecommerce.cart.application.ports.out.repositories.CartRepository;
import microservices.ecommerce.cart.core.entities.Cart;
import microservices.ecommerce.cart.infrastructure.adapters.out.entities.CartEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartRepositoryAdapter implements CartRepository {

    private final CartJpaRepository cartJpaRepository;
    private final CartMapper cartMapper;

    @Override
    public Cart save(Cart cart) {
        CartEntity entity = cartMapper.toEntity(cart);
        // Ensure bidirectional relationship is set before saving
        if (entity != null && entity.getItems() != null) {
            entity.getItems().forEach(item -> item.setCart(entity));
        }
        CartEntity savedEntity = cartJpaRepository.save(entity);
        return cartMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Cart> findById(UUID id) {
        return cartJpaRepository.findById(id).map(cartMapper::toDomain);
    }

    @Override
    public Optional<Cart> findByUserIdAndStatus(UUID userId, String status) {
        return cartJpaRepository.findByUserIdAndStatus(userId, status).map(cartMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        cartJpaRepository.deleteById(id);
    }
}

package microservices.ecommerce.cart.infrastructure.adapters.out.repositories;

import microservices.ecommerce.cart.infrastructure.adapters.out.entities.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartJpaRepository extends JpaRepository<CartEntity, UUID> {
    Optional<CartEntity> findByUserIdAndStatus(UUID userId, String status);
}

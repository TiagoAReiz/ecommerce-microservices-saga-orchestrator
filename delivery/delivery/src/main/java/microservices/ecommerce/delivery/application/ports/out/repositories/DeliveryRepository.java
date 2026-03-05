package microservices.ecommerce.delivery.application.ports.out.repositories;

import microservices.ecommerce.delivery.core.entities.Delivery;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository {
    Delivery save(Delivery delivery);

    Optional<Delivery> findById(UUID id);

    Optional<Delivery> findByOrderId(UUID orderId);
}

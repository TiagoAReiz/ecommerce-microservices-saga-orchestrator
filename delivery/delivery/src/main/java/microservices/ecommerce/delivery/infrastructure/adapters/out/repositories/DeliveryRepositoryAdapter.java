package microservices.ecommerce.delivery.infrastructure.adapters.out.repositories;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.delivery.application.mappers.DeliveryMapper;
import microservices.ecommerce.delivery.application.ports.out.repositories.DeliveryRepository;
import microservices.ecommerce.delivery.core.entities.Delivery;
import microservices.ecommerce.delivery.infrastructure.adapters.out.entities.DeliveryEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryRepositoryAdapter implements DeliveryRepository {

    private final DeliveryJpaRepository deliveryJpaRepository;
    private final DeliveryMapper deliveryMapper;

    @Override
    public Delivery save(Delivery delivery) {
        DeliveryEntity entity = deliveryMapper.toEntity(delivery);
        DeliveryEntity savedEntity = deliveryJpaRepository.save(entity);
        return deliveryMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Delivery> findById(UUID id) {
        return deliveryJpaRepository.findById(id).map(deliveryMapper::toDomain);
    }

    @Override
    public Optional<Delivery> findByOrderId(UUID orderId) {
        return deliveryJpaRepository.findByOrderId(orderId).map(deliveryMapper::toDomain);
    }
}

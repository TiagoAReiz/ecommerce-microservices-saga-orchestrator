package microservices.ecommerce.delivery.application.services;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.delivery.application.ports.in.usecases.DeliveryUseCase;
import microservices.ecommerce.delivery.application.ports.out.repositories.DeliveryRepository;
import microservices.ecommerce.delivery.core.entities.Delivery;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService implements DeliveryUseCase {

    private final DeliveryRepository deliveryRepository;

    @Override
    public Delivery scheduleDelivery(DeliveryRequest deliveryRequest) {
        Delivery delivery = new Delivery(
                UUID.randomUUID(),
                deliveryRequest.orderId(),
                deliveryRequest.carrier(),
                deliveryRequest.trackingCode(),
                "PREPARING", // Initial status
                deliveryRequest.estimatedDeliveryDate(),
                null, // Actual delivery date is null initially
                LocalDateTime.now(),
                LocalDateTime.now());

        return deliveryRepository.save(delivery);
    }

    @Override
    public Delivery getDeliveryById(UUID id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found with id: " + id));
    }

    @Override
    public Optional<Delivery> getDeliveryByOrderId(UUID orderId) {
        return deliveryRepository.findByOrderId(orderId);
    }

    @Override
    public Delivery updateDeliveryStatus(UUID id, String status) {
        Delivery delivery = getDeliveryById(id);
        delivery.setStatus(status);

        if ("DELIVERED".equalsIgnoreCase(status)) {
            delivery.setActualDeliveryDate(LocalDateTime.now());
        }

        delivery.setUpdatedAt(LocalDateTime.now());
        return deliveryRepository.save(delivery);
    }
}

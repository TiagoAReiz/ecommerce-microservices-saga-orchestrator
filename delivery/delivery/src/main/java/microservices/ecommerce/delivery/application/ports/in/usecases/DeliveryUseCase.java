package microservices.ecommerce.delivery.application.ports.in.usecases;

import microservices.ecommerce.delivery.core.entities.Delivery;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryRequest;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryUseCase {

    Delivery scheduleDelivery(DeliveryRequest deliveryRequest);

    Delivery getDeliveryById(UUID id);

    Optional<Delivery> getDeliveryByOrderId(UUID orderId);

    Delivery updateDeliveryStatus(UUID id, String status);
}

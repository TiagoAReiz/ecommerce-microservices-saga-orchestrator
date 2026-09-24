package microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param userId owner of the delivery (the customer); set by the checkout saga. Optional for back-office
 *               creations, in which case only ADMIN can read the delivery.
 */
public record DeliveryRequest(
        @NotNull UUID orderId,
        @NotBlank String carrier,
        String trackingCode,
        @NotNull @Future LocalDateTime estimatedDeliveryDate,
        UUID userId) {

    public DeliveryRequest(UUID orderId, String carrier, String trackingCode, LocalDateTime estimatedDeliveryDate) {
        this(orderId, carrier, trackingCode, estimatedDeliveryDate, null);
    }
}

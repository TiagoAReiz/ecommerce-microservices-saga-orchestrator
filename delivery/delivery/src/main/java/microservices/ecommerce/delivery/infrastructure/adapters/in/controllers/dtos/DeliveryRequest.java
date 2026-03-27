package microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryRequest(
        @NotNull UUID orderId,
        @NotBlank String carrier,
        String trackingCode,
        @NotNull @Future LocalDateTime estimatedDeliveryDate) {
}

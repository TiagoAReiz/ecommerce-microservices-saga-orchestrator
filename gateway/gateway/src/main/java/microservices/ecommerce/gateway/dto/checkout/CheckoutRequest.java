package microservices.ecommerce.gateway.dto.checkout;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckoutRequest(
        @NotNull UUID userId,
        @NotNull UUID shippingAddressId,
        @NotBlank String currency,
        @NotBlank String paymentMethod
) {}

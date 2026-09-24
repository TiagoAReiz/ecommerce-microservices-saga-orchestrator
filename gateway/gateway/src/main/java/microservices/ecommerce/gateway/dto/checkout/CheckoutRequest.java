package microservices.ecommerce.gateway.dto.checkout;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Checkout body. There is deliberately no userId: the buyer is always the authenticated user
 * (the {@code X-User-Id} header the gateway derives from the JWT), never a value the client picks.
 */
public record CheckoutRequest(
        @NotNull UUID shippingAddressId,
        @NotBlank String currency,
        @NotBlank String paymentMethod
) {}

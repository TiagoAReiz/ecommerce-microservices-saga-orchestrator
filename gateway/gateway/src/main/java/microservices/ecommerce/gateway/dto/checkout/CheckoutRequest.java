package microservices.ecommerce.gateway.dto.checkout;

import java.util.UUID;

public record CheckoutRequest(
        UUID userId,
        UUID shippingAddressId,
        String currency,
        String paymentMethod
) {}

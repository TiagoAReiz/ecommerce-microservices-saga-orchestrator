package microservices.ecommerce.gateway.dto.checkout;

import java.util.UUID;

public record CheckoutResponse(
        UUID orderId,
        String orderStatus,
        String paymentStatus,
        String transactionReference,
        UUID deliveryId
) {}

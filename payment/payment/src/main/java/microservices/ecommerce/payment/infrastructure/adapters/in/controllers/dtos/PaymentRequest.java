package microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        UUID orderId,
        BigDecimal amount,
        String currency,
        String paymentMethod) {
}

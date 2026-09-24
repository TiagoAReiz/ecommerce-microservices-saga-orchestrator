package microservices.ecommerce.gateway.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

/** @param userId owner of the payment, so the payment service can restrict reads to that user */
public record PaymentRequest(
        UUID orderId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        UUID userId
) {}

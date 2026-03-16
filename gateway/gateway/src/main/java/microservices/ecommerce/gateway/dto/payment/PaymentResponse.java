package microservices.ecommerce.gateway.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        String currency,
        String status,
        String paymentMethod,
        String transactionReference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

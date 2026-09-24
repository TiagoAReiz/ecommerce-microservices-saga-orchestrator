package microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param userId owner of the payment (the customer); set by the checkout saga. Optional for back-office
 *               creations, in which case only ADMIN can read the payment.
 */
public record PaymentRequest(
        @NotNull UUID orderId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String paymentMethod,
        UUID userId) {

    public PaymentRequest(UUID orderId, BigDecimal amount, String currency, String paymentMethod) {
        this(orderId, amount, currency, paymentMethod, null);
    }
}

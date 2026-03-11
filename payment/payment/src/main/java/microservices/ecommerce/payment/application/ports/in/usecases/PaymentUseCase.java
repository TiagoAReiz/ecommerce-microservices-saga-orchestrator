package microservices.ecommerce.payment.application.ports.in.usecases;

import microservices.ecommerce.payment.core.entities.Payment;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentRequest;
import java.util.List;
import java.util.UUID;

public interface PaymentUseCase {

    Payment processPayment(PaymentRequest paymentRequest);

    Payment getPaymentById(UUID id);

    List<Payment> getPaymentsByOrderId(UUID orderId);
}

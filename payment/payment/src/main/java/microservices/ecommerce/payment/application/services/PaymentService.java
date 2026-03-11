package microservices.ecommerce.payment.application.services;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.payment.application.ports.in.usecases.PaymentUseCase;
import microservices.ecommerce.payment.application.ports.out.repositories.PaymentRepository;
import microservices.ecommerce.payment.core.entities.Payment;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    public Payment processPayment(PaymentRequest paymentRequest) {
        // Business logic (e.g., calling external gateway Stripe/Pix) goes here
        String mockTransactionRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = new Payment(
                UUID.randomUUID(),
                paymentRequest.orderId(),
                paymentRequest.amount(),
                paymentRequest.currency(),
                "AUTHORIZED", // Assuming successful authorization
                paymentRequest.paymentMethod(),
                mockTransactionRef,
                LocalDateTime.now(),
                LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    @Override
    public Payment getPaymentById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
    }

    @Override
    public List<Payment> getPaymentsByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}

package microservices.ecommerce.payment.application.ports.out.repositories;

import microservices.ecommerce.payment.core.entities.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    List<Payment> findByOrderId(UUID orderId);
}

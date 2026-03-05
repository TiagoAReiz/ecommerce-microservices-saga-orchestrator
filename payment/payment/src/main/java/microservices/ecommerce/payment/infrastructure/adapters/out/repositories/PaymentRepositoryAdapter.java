package microservices.ecommerce.payment.infrastructure.adapters.out.repositories;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.payment.application.mappers.PaymentMapper;
import microservices.ecommerce.payment.application.ports.out.repositories.PaymentRepository;
import microservices.ecommerce.payment.core.entities.Payment;
import microservices.ecommerce.payment.infrastructure.adapters.out.entities.PaymentEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = paymentMapper.toEntity(payment);
        PaymentEntity savedEntity = paymentJpaRepository.save(entity);
        return paymentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return paymentJpaRepository.findById(id).map(paymentMapper::toDomain);
    }

    @Override
    public List<Payment> findByOrderId(UUID orderId) {
        return paymentJpaRepository.findByOrderId(orderId).stream()
                .map(paymentMapper::toDomain)
                .collect(Collectors.toList());
    }
}

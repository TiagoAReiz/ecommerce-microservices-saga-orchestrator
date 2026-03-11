package microservices.ecommerce.payment.application.mappers;

import microservices.ecommerce.payment.core.entities.Payment;
import microservices.ecommerce.payment.infrastructure.adapters.out.entities.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentEntity toEntity(Payment domain) {
        if (domain == null)
            return null;
        return PaymentEntity.builder()
                .id(domain.getId())
                .orderId(domain.getOrderId())
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .status(domain.getStatus())
                .paymentMethod(domain.getPaymentMethod())
                .transactionReference(domain.getTransactionReference())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Payment toDomain(PaymentEntity entity) {
        if (entity == null)
            return null;
        return new Payment(
                entity.getId(),
                entity.getOrderId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getPaymentMethod(),
                entity.getTransactionReference(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentResponse toResponse(
            Payment domain) {
        if (domain == null)
            return null;
        return new microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentResponse(
                domain.getId(),
                domain.getOrderId(),
                domain.getAmount(),
                domain.getCurrency(),
                domain.getStatus(),
                domain.getPaymentMethod(),
                domain.getTransactionReference(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}

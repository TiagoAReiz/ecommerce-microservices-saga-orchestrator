package microservices.ecommerce.payment.application.services;

import microservices.ecommerce.payment.application.ports.out.repositories.PaymentRepository;
import microservices.ecommerce.payment.core.entities.Payment;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
    }

    @Test
    void processPayment_validRequest_savesWithAuthorizedStatus() {
        PaymentRequest request = new PaymentRequest(orderId, BigDecimal.valueOf(150.00), "BRL", "CREDIT_CARD");
        Payment saved = buildPayment(paymentId, orderId, "AUTHORIZED");
        when(paymentRepository.save(any())).thenReturn(saved);

        Payment result = paymentService.processPayment(request);

        assertThat(result.getStatus()).isEqualTo("AUTHORIZED");
        assertThat(result.getOrderId()).isEqualTo(orderId);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment toSave = captor.getValue();
        assertThat(toSave.getOrderId()).isEqualTo(orderId);
        assertThat(toSave.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(toSave.getCurrency()).isEqualTo("BRL");
        assertThat(toSave.getPaymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(toSave.getTransactionReference()).startsWith("TXN-");
    }

    @Test
    void processPayment_generatesUniqueTransactionReferences() {
        PaymentRequest request = new PaymentRequest(orderId, BigDecimal.TEN, "BRL", "CREDIT_CARD");
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment first = paymentService.processPayment(request);
        Payment second = paymentService.processPayment(request);

        assertThat(first.getTransactionReference()).isNotEqualTo(second.getTransactionReference());
    }

    @Test
    void getPaymentById_existingId_returnsPayment() {
        Payment payment = buildPayment(paymentId, orderId, "AUTHORIZED");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentById(paymentId);

        assertThat(result.getId()).isEqualTo(paymentId);
    }

    @Test
    void getPaymentById_nonExistingId_throwsRuntimeException() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(paymentId.toString());
    }

    @Test
    void getPaymentsByOrderId_returnsAllPaymentsForOrder() {
        List<Payment> payments = List.of(
                buildPayment(UUID.randomUUID(), orderId, "AUTHORIZED"),
                buildPayment(UUID.randomUUID(), orderId, "REFUNDED")
        );
        when(paymentRepository.findByOrderId(orderId)).thenReturn(payments);

        List<Payment> result = paymentService.getPaymentsByOrderId(orderId);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getOrderId().equals(orderId));
    }

    @Test
    void getPaymentsByOrderId_noPayments_returnsEmptyList() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());

        List<Payment> result = paymentService.getPaymentsByOrderId(orderId);

        assertThat(result).isEmpty();
    }

    private Payment buildPayment(UUID id, UUID orderId, String status) {
        return new Payment(id, orderId, BigDecimal.TEN, "BRL", status,
                "CREDIT_CARD", "TXN-" + id.toString().substring(0, 8).toUpperCase(),
                LocalDateTime.now(), LocalDateTime.now());
    }
}

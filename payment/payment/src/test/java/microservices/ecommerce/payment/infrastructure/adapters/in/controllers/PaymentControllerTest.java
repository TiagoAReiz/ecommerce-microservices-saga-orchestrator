package microservices.ecommerce.payment.infrastructure.adapters.in.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import microservices.ecommerce.payment.application.mappers.PaymentMapper;
import microservices.ecommerce.payment.application.ports.in.usecases.PaymentUseCase;
import microservices.ecommerce.payment.core.entities.Payment;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentRequest;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentUseCase paymentUseCase;

    @MockitoBean
    private PaymentMapper paymentMapper;

    @Test
    void processPayment_validRequest_returns201WithBody() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(orderId, BigDecimal.valueOf(99.90), "BRL", "CREDIT_CARD");
        Payment payment = buildPayment(paymentId, orderId);
        PaymentResponse response = buildResponse(paymentId, orderId);

        when(paymentUseCase.processPayment(any())).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    void getPaymentById_existingId_returns200() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment payment = buildPayment(paymentId, orderId);
        PaymentResponse response = buildResponse(paymentId, orderId);

        when(paymentUseCase.getPaymentById(paymentId)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()));
    }

    @Test
    void getPaymentsByOrderId_returns200WithList() throws Exception {
        UUID orderId = UUID.randomUUID();
        Payment p1 = buildPayment(UUID.randomUUID(), orderId);
        Payment p2 = buildPayment(UUID.randomUUID(), orderId);
        PaymentResponse r1 = buildResponse(p1.getId(), orderId);
        PaymentResponse r2 = buildResponse(p2.getId(), orderId);

        when(paymentUseCase.getPaymentsByOrderId(orderId)).thenReturn(List.of(p1, p2));
        when(paymentMapper.toResponse(p1)).thenReturn(r1);
        when(paymentMapper.toResponse(p2)).thenReturn(r2);

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getPaymentById_serviceThrows_returns500() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentUseCase.getPaymentById(paymentId))
                .thenThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().is5xxServerError());
    }

    private Payment buildPayment(UUID id, UUID orderId) {
        return new Payment(id, orderId, BigDecimal.TEN, "BRL", "AUTHORIZED",
                "CREDIT_CARD", "TXN-TEST", LocalDateTime.now(), LocalDateTime.now());
    }

    private PaymentResponse buildResponse(UUID id, UUID orderId) {
        return new PaymentResponse(id, orderId, BigDecimal.TEN, "BRL", "AUTHORIZED",
                "CREDIT_CARD", "TXN-TEST", null, null);
    }
}

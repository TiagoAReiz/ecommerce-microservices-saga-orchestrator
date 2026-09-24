package microservices.ecommerce.payment.infrastructure.adapters.in.controllers;

import microservices.ecommerce.payment.application.mappers.PaymentMapper;
import microservices.ecommerce.payment.application.ports.in.usecases.PaymentUseCase;
import microservices.ecommerce.payment.core.entities.Payment;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ownership (X-User-Id from the gateway) and role checks of the payment endpoints. */
@WebMvcTest(PaymentController.class)
class PaymentAccessControlTest {

    private static final String USER_ID = "X-User-Id";
    private static final String ROLES = "X-User-Roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentUseCase paymentUseCase;

    @MockitoBean
    private PaymentMapper paymentMapper;

    private final UUID owner = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = new Payment(paymentId, orderId, BigDecimal.TEN, "BRL", "AUTHORIZED", "CREDIT_CARD", "TXN",
                LocalDateTime.now(), LocalDateTime.now());
        payment.setUserId(owner);
        when(paymentUseCase.getPaymentById(paymentId)).thenReturn(payment);
        when(paymentUseCase.getPaymentsByOrderId(orderId)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(any())).thenReturn(new PaymentResponse(paymentId, orderId, BigDecimal.TEN,
                "BRL", "AUTHORIZED", "CREDIT_CARD", "TXN", null, null));
    }

    @Test
    void getPayment_byOwner_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", paymentId).header(USER_ID, owner.toString()).header(ROLES, "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void getPayment_byAnotherUser_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", paymentId)
                        .header(USER_ID, UUID.randomUUID().toString()).header(ROLES, "USER"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayment_byAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", paymentId)
                        .header(USER_ID, UUID.randomUUID().toString()).header(ROLES, "USER,ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getPayment_withoutRecordedOwner_isOnlyVisibleToAdmin() throws Exception {
        payment.setUserId(null);

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId).header(USER_ID, owner.toString()).header(ROLES, "USER"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/payments/{id}", paymentId).header(USER_ID, owner.toString()).header(ROLES, "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void paymentsByOrder_filtersOutOtherUsersPayments() throws Exception {
        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId).header(USER_ID, owner.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId).header(USER_ID, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        // the cancellation saga calls internally, without identity headers
        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void processPayment_byUser_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header(USER_ID, owner.toString()).header(ROLES, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","amount":10,"currency":"BRL","paymentMethod":"PIX"}
                                """.formatted(orderId)))
                .andExpect(status().isForbidden());

        verify(paymentUseCase, never()).processPayment(any());
    }
}

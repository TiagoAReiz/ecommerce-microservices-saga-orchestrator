package microservices.ecommerce.delivery.infrastructure.adapters.in.controllers;

import microservices.ecommerce.delivery.application.mappers.DeliveryMapper;
import microservices.ecommerce.delivery.application.ports.in.usecases.DeliveryUseCase;
import microservices.ecommerce.delivery.core.entities.Delivery;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ownership (X-User-Id from the gateway) and role checks of the delivery endpoints. */
@WebMvcTest(DeliveryController.class)
class DeliveryAccessControlTest {

    private static final String USER_ID = "X-User-Id";
    private static final String ROLES = "X-User-Roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryUseCase deliveryUseCase;

    @MockitoBean
    private DeliveryMapper deliveryMapper;

    private final UUID owner = UUID.randomUUID();
    private final UUID deliveryId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Delivery delivery = new Delivery(deliveryId, orderId, "DHL", "TRK", "PREPARING",
                LocalDateTime.now().plusDays(7), null, LocalDateTime.now(), LocalDateTime.now());
        delivery.setUserId(owner);
        when(deliveryUseCase.getDeliveryById(deliveryId)).thenReturn(delivery);
        when(deliveryUseCase.getDeliveryByOrderId(orderId)).thenReturn(Optional.of(delivery));
        when(deliveryMapper.toResponse(any())).thenReturn(new DeliveryResponse(deliveryId, orderId, "DHL", "TRK",
                "PREPARING", null, null, null, null));
    }

    @Test
    void getDelivery_byOwner_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/{id}", deliveryId).header(USER_ID, owner.toString()).header(ROLES, "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void getDelivery_byAnotherUser_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/{id}", deliveryId)
                        .header(USER_ID, UUID.randomUUID().toString()).header(ROLES, "USER"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/deliveries/order/{orderId}", orderId)
                        .header(USER_ID, UUID.randomUUID().toString()).header(ROLES, "USER"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDelivery_byAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/{id}", deliveryId)
                        .header(USER_ID, UUID.randomUUID().toString()).header(ROLES, "USER,ADMIN"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/deliveries/order/{orderId}", orderId)
                        .header(USER_ID, UUID.randomUUID().toString()).header(ROLES, "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_byUser_returns403_byAdminOrInternal_returns200() throws Exception {
        mockMvc.perform(patch("/api/v1/deliveries/{id}/status", deliveryId).param("status", "DELIVERED")
                        .header(USER_ID, owner.toString()).header(ROLES, "USER"))
                .andExpect(status().isForbidden());
        verify(deliveryUseCase, never()).updateDeliveryStatus(any(), any());

        mockMvc.perform(patch("/api/v1/deliveries/{id}/status", deliveryId).param("status", "DELIVERED")
                        .header(USER_ID, owner.toString()).header(ROLES, "ADMIN"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/deliveries/{id}/status", deliveryId).param("status", "CANCELLED"))
                .andExpect(status().isOk());
    }
}

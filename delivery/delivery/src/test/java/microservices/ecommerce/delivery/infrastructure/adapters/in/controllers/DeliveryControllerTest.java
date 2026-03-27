package microservices.ecommerce.delivery.infrastructure.adapters.in.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import microservices.ecommerce.delivery.application.mappers.DeliveryMapper;
import microservices.ecommerce.delivery.application.ports.in.usecases.DeliveryUseCase;
import microservices.ecommerce.delivery.core.entities.Delivery;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryRequest;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeliveryController.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryUseCase deliveryUseCase;

    @MockitoBean
    private DeliveryMapper deliveryMapper;

    @Test
    void scheduleDelivery_validRequest_returns201() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = buildDelivery(deliveryId, orderId, "PREPARING");
        DeliveryResponse response = buildResponse(deliveryId, orderId, "PREPARING");

        when(deliveryUseCase.scheduleDelivery(any())).thenReturn(delivery);
        when(deliveryMapper.toResponse(delivery)).thenReturn(response);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        DeliveryRequest request = new DeliveryRequest(orderId, "DHL", "TRK-001",
                LocalDateTime.now().plusDays(7));

        mockMvc.perform(post("/api/v1/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(deliveryId.toString()))
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void getDeliveryById_existingId_returns200() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Delivery delivery = buildDelivery(deliveryId, orderId, "PREPARING");
        DeliveryResponse response = buildResponse(deliveryId, orderId, "PREPARING");

        when(deliveryUseCase.getDeliveryById(deliveryId)).thenReturn(delivery);
        when(deliveryMapper.toResponse(delivery)).thenReturn(response);

        mockMvc.perform(get("/api/v1/deliveries/{id}", deliveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deliveryId.toString()));
    }

    @Test
    void getDeliveryByOrderId_existing_returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = buildDelivery(deliveryId, orderId, "SHIPPED");
        DeliveryResponse response = buildResponse(deliveryId, orderId, "SHIPPED");

        when(deliveryUseCase.getDeliveryByOrderId(orderId)).thenReturn(Optional.of(delivery));
        when(deliveryMapper.toResponse(delivery)).thenReturn(response);

        mockMvc.perform(get("/api/v1/deliveries/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()));
    }

    @Test
    void getDeliveryByOrderId_notFound_returns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(deliveryUseCase.getDeliveryByOrderId(orderId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/deliveries/order/{orderId}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDeliveryStatus_returns200() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Delivery delivery = buildDelivery(deliveryId, orderId, "SHIPPED");
        DeliveryResponse response = buildResponse(deliveryId, orderId, "SHIPPED");

        when(deliveryUseCase.updateDeliveryStatus(eq(deliveryId), eq("SHIPPED"))).thenReturn(delivery);
        when(deliveryMapper.toResponse(delivery)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/deliveries/{id}/status", deliveryId)
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void getDeliveryById_serviceThrows_returns500() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryUseCase.getDeliveryById(deliveryId))
                .thenThrow(new RuntimeException("Delivery not found"));

        mockMvc.perform(get("/api/v1/deliveries/{id}", deliveryId))
                .andExpect(status().is5xxServerError());
    }

    private Delivery buildDelivery(UUID id, UUID orderId, String status) {
        return new Delivery(id, orderId, "DHL", "TRK-001", status,
                LocalDateTime.now().plusDays(7), null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private DeliveryResponse buildResponse(UUID id, UUID orderId, String status) {
        return new DeliveryResponse(id, orderId, "DHL", "TRK-001", status,
                null, null, null, null);
    }
}

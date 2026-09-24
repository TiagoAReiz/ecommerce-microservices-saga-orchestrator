package microservices.ecommerce.order.infrastructure.adapters.in.controllers;

import microservices.ecommerce.order.application.mappers.OrderMapper;
import microservices.ecommerce.order.application.ports.in.usecases.OrderUseCase;
import microservices.ecommerce.order.core.entities.Order;
import microservices.ecommerce.order.core.exceptions.ResourceNotFoundException;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ownership (X-User-Id from the gateway) and role checks of the order endpoints. */
@WebMvcTest(OrderController.class)
class OrderAccessControlTest {

    private static final String USER_ID = "X-User-Id";
    private static final String ROLES = "X-User-Roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderUseCase orderUseCase;

    @MockitoBean
    private OrderMapper orderMapper;

    private final UUID owner = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Order order = new Order(orderId, owner, "CREATED", BigDecimal.ZERO, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now(), List.of());
        when(orderUseCase.getOrderById(orderId)).thenReturn(order);
        when(orderMapper.toResponse(any())).thenReturn(new OrderResponse(orderId, owner, "CREATED",
                BigDecimal.ZERO, null, null, null, List.of()));
    }

    @Test
    void getOrder_byOwner_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", orderId).header(USER_ID, owner.toString()).header(ROLES, "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void getOrder_byAnotherUser_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header(USER_ID, UUID.randomUUID().toString()).header(ROLES, "USER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void getOrder_byAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header(USER_ID, UUID.randomUUID().toString()).header(ROLES, "USER,ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrder_internalCallWithoutIdentity_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)).andExpect(status().isOk());
    }

    @Test
    void getOrder_unknownId_returns404() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(orderUseCase.getOrderById(unknown)).thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/v1/orders/{id}", unknown).header(USER_ID, owner.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listOrdersOfAnotherUser_returns403_butAdminMayList() throws Exception {
        UUID other = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/orders/user/{userId}", other).header(USER_ID, owner.toString()).header(ROLES, "USER"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/orders/user/{userId}", other).header(USER_ID, owner.toString()).header(ROLES, "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_byUser_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId).param("status", "SHIPPED")
                        .header(USER_ID, owner.toString()).header(ROLES, "USER"))
                .andExpect(status().isForbidden());

        verify(orderUseCase, never()).updateOrderStatus(any(), any());
    }

    @Test
    void updateStatus_byAdmin_returns200() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId).param("status", "SHIPPED")
                        .header(USER_ID, UUID.randomUUID().toString()).header(ROLES, "USER,ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void createOrder_byUser_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header(USER_ID, owner.toString()).header(ROLES, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","shippingAddressId":"%s","items":[{"productId":"%s","quantity":1}]}
                                """.formatted(owner, UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isForbidden());

        verify(orderUseCase, never()).createOrder(any());
    }
}

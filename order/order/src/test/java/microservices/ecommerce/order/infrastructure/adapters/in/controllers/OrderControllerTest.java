package microservices.ecommerce.order.infrastructure.adapters.in.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import microservices.ecommerce.order.application.mappers.OrderMapper;
import microservices.ecommerce.order.application.ports.in.usecases.OrderUseCase;
import microservices.ecommerce.order.core.entities.Order;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderItemResponse;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderRequest;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderUseCase orderUseCase;

    @MockitoBean
    private OrderMapper orderMapper;

    @Test
    void createOrder_validRequest_returns201WithBody() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        OrderRequest request = new OrderRequest(userId, addressId, List.of());
        Order order = buildOrder(orderId, userId, "CREATED");
        OrderResponse response = buildOrderResponse(orderId, userId, "CREATED");

        when(orderUseCase.createOrder(any())).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getOrderById_existingOrder_returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = buildOrder(orderId, userId, "CREATED");
        OrderResponse response = buildOrderResponse(orderId, userId, "CREATED");

        when(orderUseCase.getOrderById(orderId)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void getOrdersByUserId_returns200WithList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        Order order1 = buildOrder(orderId1, userId, "CREATED");
        Order order2 = buildOrder(orderId2, userId, "SHIPPED");
        OrderResponse resp1 = buildOrderResponse(orderId1, userId, "CREATED");
        OrderResponse resp2 = buildOrderResponse(orderId2, userId, "SHIPPED");

        when(orderUseCase.getOrdersByUserId(userId)).thenReturn(List.of(order1, order2));
        when(orderMapper.toResponse(order1)).thenReturn(resp1);
        when(orderMapper.toResponse(order2)).thenReturn(resp2);

        mockMvc.perform(get("/api/v1/orders/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateOrderStatus_existingOrder_returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order updated = buildOrder(orderId, userId, "SHIPPED");
        OrderResponse response = buildOrderResponse(orderId, userId, "SHIPPED");

        when(orderUseCase.updateOrderStatus(eq(orderId), eq("SHIPPED"))).thenReturn(updated);
        when(orderMapper.toResponse(updated)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void getOrderById_serviceThrows_returns500() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderUseCase.getOrderById(orderId)).thenThrow(new RuntimeException("Order not found"));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().is5xxServerError());
    }

    private Order buildOrder(UUID id, UUID userId, String status) {
        return new Order(id, userId, status, BigDecimal.ZERO, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now(), List.of());
    }

    private OrderResponse buildOrderResponse(UUID id, UUID userId, String status) {
        return new OrderResponse(id, userId, status, BigDecimal.ZERO, UUID.randomUUID(),
                null, null, List.of());
    }
}

package microservices.ecommerce.cart.infrastructure.adapters.in.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import microservices.ecommerce.cart.application.mappers.CartMapper;
import microservices.ecommerce.cart.application.ports.in.usecases.CartUseCase;
import microservices.ecommerce.cart.core.entities.Cart;
import microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos.CartItemRequest;
import microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos.CartItemResponse;
import microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos.CartResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartUseCase cartUseCase;

    @MockitoBean
    private CartMapper cartMapper;

    @Test
    void addItemToCart_validRequest_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Cart cart = buildCart(userId);
        CartResponse response = buildCartResponse(userId, productId);

        when(cartUseCase.addItemToCart(eq(userId), any())).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(response);

        CartItemRequest request = new CartItemRequest(productId, 2, BigDecimal.TEN);
        mockMvc.perform(post("/api/v1/carts/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void getCartByUserId_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Cart cart = buildCart(userId);
        CartResponse response = buildCartResponse(userId, productId);

        when(cartUseCase.getCartByUserId(userId)).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(response);

        mockMvc.perform(get("/api/v1/carts/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void removeItemFromCart_returns200WithUpdatedCart() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Cart cart = buildCart(userId);
        CartResponse response = buildCartResponse(userId, null);

        when(cartUseCase.removeItemFromCart(userId, productId)).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/carts/{userId}/items/{productId}", userId, productId))
                .andExpect(status().isOk());
    }

    @Test
    void clearCart_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(cartUseCase).clearCart(userId);

        mockMvc.perform(delete("/api/v1/carts/{userId}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    void checkoutCart_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(cartUseCase).checkoutCart(userId);

        mockMvc.perform(post("/api/v1/carts/{userId}/checkout", userId))
                .andExpect(status().isOk());
    }

    @Test
    void checkoutCart_serviceThrows_returns500() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new RuntimeException("Cannot checkout empty cart"))
                .when(cartUseCase).checkoutCart(userId);

        mockMvc.perform(post("/api/v1/carts/{userId}/checkout", userId))
                .andExpect(status().is5xxServerError());
    }

    private Cart buildCart(UUID userId) {
        return new Cart(UUID.randomUUID(), userId, "ACTIVE",
                LocalDateTime.now(), LocalDateTime.now(), new ArrayList<>());
    }

    private CartResponse buildCartResponse(UUID userId, UUID productId) {
        List<CartItemResponse> items = productId != null
                ? List.of(new CartItemResponse(UUID.randomUUID(), productId, 2, BigDecimal.TEN))
                : List.of();
        return new CartResponse(UUID.randomUUID(), userId, "ACTIVE", null, null, items);
    }
}

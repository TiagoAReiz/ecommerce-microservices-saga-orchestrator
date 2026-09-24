package microservices.ecommerce.inventory.infrastructure.adapters.in.controllers;

import microservices.ecommerce.inventory.application.mappers.InventoryMapper;
import microservices.ecommerce.inventory.application.ports.in.usecases.InventoryUseCase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stock changes are admin/internal only (defence in depth behind the gateway rule). */
@WebMvcTest(InventoryController.class)
class InventoryAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryUseCase inventoryUseCase;

    @MockitoBean
    private InventoryMapper inventoryMapper;

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/inventory/stock", "/api/v1/inventory/reserve", "/api/v1/inventory/release"})
    void stockChange_byUser_returns403(String path) throws Exception {
        mockMvc.perform(post(path)
                        .header("X-User-Id", UUID.randomUUID().toString()).header("X-User-Roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantityAvailable":5,"quantityReserved":1}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());

        verify(inventoryUseCase, never()).addStock(any(), anyInt());
        verify(inventoryUseCase, never()).reserveStock(any(), anyInt());
        verify(inventoryUseCase, never()).releaseStock(any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/inventory/stock", "/api/v1/inventory/reserve", "/api/v1/inventory/release"})
    void stockChange_byAdmin_isAllowed(String path) throws Exception {
        mockMvc.perform(post(path)
                        .header("X-User-Id", UUID.randomUUID().toString()).header("X-User-Roles", "USER,ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantityAvailable":5,"quantityReserved":1}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
    }
}

package microservices.ecommerce.inventory.infrastructure.adapters.in.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import microservices.ecommerce.inventory.application.mappers.InventoryMapper;
import microservices.ecommerce.inventory.application.ports.in.usecases.InventoryUseCase;
import microservices.ecommerce.inventory.core.entities.Inventory;
import microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.dtos.InventoryRequest;
import microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.dtos.InventoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryUseCase inventoryUseCase;

    @MockitoBean
    private InventoryMapper inventoryMapper;

    @Test
    void addStock_validRequest_returns200() throws Exception {
        UUID productId = UUID.randomUUID();
        Inventory inventory = buildInventory(productId, 10, 0);
        InventoryResponse response = buildResponse(productId, 10, 0);

        when(inventoryUseCase.addStock(eq(productId), eq(10))).thenReturn(inventory);
        when(inventoryMapper.toResponse(inventory)).thenReturn(response);

        InventoryRequest request = new InventoryRequest(productId, 10, 0);
        mockMvc.perform(post("/api/v1/inventory/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantityAvailable").value(10));
    }

    @Test
    void reserveStock_validRequest_returns200() throws Exception {
        UUID productId = UUID.randomUUID();
        Inventory inventory = buildInventory(productId, 7, 3);
        InventoryResponse response = buildResponse(productId, 7, 3);

        when(inventoryUseCase.reserveStock(eq(productId), eq(3))).thenReturn(inventory);
        when(inventoryMapper.toResponse(inventory)).thenReturn(response);

        InventoryRequest request = new InventoryRequest(productId, 0, 3);
        mockMvc.perform(post("/api/v1/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityReserved").value(3));
    }

    @Test
    void releaseStock_validRequest_returns200() throws Exception {
        UUID productId = UUID.randomUUID();
        Inventory inventory = buildInventory(productId, 10, 0);
        InventoryResponse response = buildResponse(productId, 10, 0);

        when(inventoryUseCase.releaseStock(eq(productId), eq(2))).thenReturn(inventory);
        when(inventoryMapper.toResponse(inventory)).thenReturn(response);

        InventoryRequest request = new InventoryRequest(productId, 0, 2);
        mockMvc.perform(post("/api/v1/inventory/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getInventoryByProductId_returns200() throws Exception {
        UUID productId = UUID.randomUUID();
        Inventory inventory = buildInventory(productId, 5, 1);
        InventoryResponse response = buildResponse(productId, 5, 1);

        when(inventoryUseCase.getInventoryByProductId(productId)).thenReturn(inventory);
        when(inventoryMapper.toResponse(inventory)).thenReturn(response);

        mockMvc.perform(get("/api/v1/inventory/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantityAvailable").value(5));
    }

    @Test
    void reserveStock_serviceThrows_returns500() throws Exception {
        UUID productId = UUID.randomUUID();
        when(inventoryUseCase.reserveStock(any(), any()))
                .thenThrow(new RuntimeException("Insufficient stock"));

        InventoryRequest request = new InventoryRequest(productId, 0, 999);
        mockMvc.perform(post("/api/v1/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    private Inventory buildInventory(UUID productId, int available, int reserved) {
        return new Inventory(UUID.randomUUID(), productId, available, reserved, LocalDateTime.now());
    }

    private InventoryResponse buildResponse(UUID productId, int available, int reserved) {
        return new InventoryResponse(UUID.randomUUID(), productId, available, reserved, null);
    }
}

package microservices.ecommerce.products.infrastructure.adapters.in.controllers;

import microservices.ecommerce.products.application.mappers.ProductMapper;
import microservices.ecommerce.products.application.ports.in.usecases.ProductUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The catalogue is public for reading; writes need ADMIN (defence in depth behind the gateway rule). */
@WebMvcTest(ProductController.class)
class ProductAccessControlTest {

    private static final String BODY = """
            {"name":"Mug","description":"Blue","price":10.0,"sku":"MUG-1","active":true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductUseCase productUseCase;

    @MockitoBean
    private ProductMapper productMapper;

    @Test
    void listProducts_isPublic() throws Exception {
        when(productUseCase.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());
    }

    @Test
    void writes_byUser_return403() throws Exception {
        UUID id = UUID.randomUUID();
        String user = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/products").header("X-User-Id", user).header("X-User-Roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/products/{id}", id).header("X-User-Id", user).header("X-User-Roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/products/{id}", id).header("X-User-Id", user).header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());

        verify(productUseCase, never()).createProduct(any());
        verify(productUseCase, never()).updateProduct(any(), any());
        verify(productUseCase, never()).deleteProduct(any());
    }

    @Test
    void writes_byAdmin_areAllowed() throws Exception {
        String admin = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/products").header("X-User-Id", admin).header("X-User-Roles", "USER,ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/v1/products/{id}", UUID.randomUUID())
                        .header("X-User-Id", admin).header("X-User-Roles", "ADMIN"))
                .andExpect(status().isNoContent());
    }
}

package microservices.ecommerce.products.infrastructure.adapters.in.controllers;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.products.application.mappers.ProductMapper;
import microservices.ecommerce.products.application.ports.in.usecases.ProductUseCase;
import microservices.ecommerce.products.core.entities.Product;
import microservices.ecommerce.products.infrastructure.adapters.in.controllers.dtos.ProductRequest;
import microservices.ecommerce.products.infrastructure.adapters.in.controllers.dtos.ProductResponse;
import microservices.ecommerce.products.infrastructure.adapters.in.controllers.security.Caller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** The catalogue is public for reading; creating, updating and deleting products requires ADMIN. */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductUseCase productUseCase;
    private final ProductMapper productMapper;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @RequestBody ProductRequest request) {
        Caller.from(userIdHeader, rolesHeader).requireAdminOrInternal();
        Product product = productMapper.toDomain(request);
        Product savedProduct = productUseCase.createProduct(product);
        return new ResponseEntity<>(productMapper.toResponse(savedProduct), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        Product product = productUseCase.getProductById(id);
        return ResponseEntity.ok(productMapper.toResponse(product));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> responses = productUseCase.getAllProducts().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @PathVariable UUID id, @RequestBody ProductRequest request) {
        Caller.from(userIdHeader, rolesHeader).requireAdminOrInternal();
        Product productUpdate = productMapper.toDomain(request);
        Product updatedProduct = productUseCase.updateProduct(id, productUpdate);
        return ResponseEntity.ok(productMapper.toResponse(updatedProduct));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @PathVariable UUID id) {
        Caller.from(userIdHeader, rolesHeader).requireAdminOrInternal();
        productUseCase.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

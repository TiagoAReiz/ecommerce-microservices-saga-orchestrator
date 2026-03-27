package microservices.ecommerce.inventory.infrastructure.adapters.in.controllers;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.inventory.application.mappers.InventoryMapper;
import microservices.ecommerce.inventory.application.ports.in.usecases.InventoryUseCase;
import microservices.ecommerce.inventory.core.entities.Inventory;
import microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.dtos.InventoryRequest;
import microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.dtos.InventoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryUseCase inventoryUseCase;
    private final InventoryMapper inventoryMapper;

    @PostMapping("/stock")
    public ResponseEntity<InventoryResponse> addStock(@Valid @RequestBody InventoryRequest request) {
        Inventory inventory = inventoryUseCase.addStock(request.productId(), request.quantityAvailable()); // using
                                                                                                           // quantityAvailable
                                                                                                           // field as
                                                                                                           // 'amount to
                                                                                                           // add' from
                                                                                                           // request
        return ResponseEntity.ok(inventoryMapper.toResponse(inventory));
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserveStock(@Valid @RequestBody InventoryRequest request) {
        Inventory inventory = inventoryUseCase.reserveStock(request.productId(), request.quantityReserved()); // using
                                                                                                              // quantityReserved
                                                                                                              // field
                                                                                                              // as
                                                                                                              // 'amount
                                                                                                              // to
                                                                                                              // reserve'
                                                                                                              // from
                                                                                                              // request
        return ResponseEntity.ok(inventoryMapper.toResponse(inventory));
    }

    @PostMapping("/release")
    public ResponseEntity<InventoryResponse> releaseStock(@Valid @RequestBody InventoryRequest request) {
        Inventory inventory = inventoryUseCase.releaseStock(request.productId(), request.quantityReserved()); // again,
                                                                                                              // using
                                                                                                              // as
                                                                                                              // amount
        return ResponseEntity.ok(inventoryMapper.toResponse(inventory));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable UUID productId) {
        Inventory inventory = inventoryUseCase.getInventoryByProductId(productId);
        return ResponseEntity.ok(inventoryMapper.toResponse(inventory));
    }
}

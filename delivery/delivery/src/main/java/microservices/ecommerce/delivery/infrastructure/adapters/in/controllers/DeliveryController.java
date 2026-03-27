package microservices.ecommerce.delivery.infrastructure.adapters.in.controllers;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.delivery.application.mappers.DeliveryMapper;
import microservices.ecommerce.delivery.application.ports.in.usecases.DeliveryUseCase;
import microservices.ecommerce.delivery.core.entities.Delivery;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryRequest;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryUseCase deliveryUseCase;
    private final DeliveryMapper deliveryMapper;

    @PostMapping
    public ResponseEntity<DeliveryResponse> scheduleDelivery(@Valid @RequestBody DeliveryRequest request) {
        Delivery delivery = deliveryUseCase.scheduleDelivery(request);
        return new ResponseEntity<>(deliveryMapper.toResponse(delivery), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryResponse> getDeliveryById(@PathVariable UUID id) {
        Delivery delivery = deliveryUseCase.getDeliveryById(id);
        return ResponseEntity.ok(deliveryMapper.toResponse(delivery));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryResponse> getDeliveryByOrderId(@PathVariable UUID orderId) {
        return deliveryUseCase.getDeliveryByOrderId(orderId)
                .map(delivery -> ResponseEntity.ok(deliveryMapper.toResponse(delivery)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeliveryResponse> updateDeliveryStatus(@PathVariable UUID id, @RequestParam String status) {
        Delivery delivery = deliveryUseCase.updateDeliveryStatus(id, status);
        return ResponseEntity.ok(deliveryMapper.toResponse(delivery));
    }
}

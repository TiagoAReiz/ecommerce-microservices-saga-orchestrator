package microservices.ecommerce.delivery.infrastructure.adapters.in.controllers;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.delivery.application.mappers.DeliveryMapper;
import microservices.ecommerce.delivery.application.ports.in.usecases.DeliveryUseCase;
import microservices.ecommerce.delivery.core.entities.Delivery;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryRequest;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryResponse;
import microservices.ecommerce.delivery.core.exceptions.ResourceNotFoundException;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.security.Caller;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Deliveries are visible to their owner (or ADMIN); another user's delivery answers 404. Scheduling and
 * status changes are done by the saga (internal) or ADMIN.
 */
@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryUseCase deliveryUseCase;
    private final DeliveryMapper deliveryMapper;

    @PostMapping
    public ResponseEntity<DeliveryResponse> scheduleDelivery(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @Valid @RequestBody DeliveryRequest request) {
        Caller.from(userIdHeader, rolesHeader).requireAdminOrInternal();
        Delivery delivery = deliveryUseCase.scheduleDelivery(request);
        return new ResponseEntity<>(deliveryMapper.toResponse(delivery), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryResponse> getDeliveryById(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @PathVariable UUID id) {
        Delivery delivery = deliveryUseCase.getDeliveryById(id);
        if (!Caller.from(userIdHeader, rolesHeader).canAccess(delivery.getUserId())) {
            throw new ResourceNotFoundException("Delivery not found with id: " + id);
        }
        return ResponseEntity.ok(deliveryMapper.toResponse(delivery));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryResponse> getDeliveryByOrderId(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @PathVariable UUID orderId) {
        Caller caller = Caller.from(userIdHeader, rolesHeader);
        return deliveryUseCase.getDeliveryByOrderId(orderId)
                .filter(delivery -> caller.canAccess(delivery.getUserId()))
                .map(delivery -> ResponseEntity.ok(deliveryMapper.toResponse(delivery)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeliveryResponse> updateDeliveryStatus(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @PathVariable UUID id, @RequestParam String status) {
        Caller.from(userIdHeader, rolesHeader).requireAdminOrInternal();
        Delivery delivery = deliveryUseCase.updateDeliveryStatus(id, status);
        return ResponseEntity.ok(deliveryMapper.toResponse(delivery));
    }
}

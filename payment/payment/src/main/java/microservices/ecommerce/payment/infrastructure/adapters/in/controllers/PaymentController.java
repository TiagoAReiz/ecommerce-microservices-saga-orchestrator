package microservices.ecommerce.payment.infrastructure.adapters.in.controllers;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.payment.application.mappers.PaymentMapper;
import microservices.ecommerce.payment.application.ports.in.usecases.PaymentUseCase;
import microservices.ecommerce.payment.core.entities.Payment;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentRequest;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentResponse;
import microservices.ecommerce.payment.core.exceptions.ResourceNotFoundException;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.security.Caller;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payments are visible to their owner (or ADMIN); another user's payment answers 404. Creating payments
 * is done by the checkout saga (internal) or ADMIN.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentUseCase paymentUseCase;
    private final PaymentMapper paymentMapper;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @Valid @RequestBody PaymentRequest request) {
        Caller.from(userIdHeader, rolesHeader).requireAdminOrInternal();
        Payment payment = paymentUseCase.processPayment(request);
        return new ResponseEntity<>(paymentMapper.toResponse(payment), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @PathVariable UUID id) {
        Payment payment = paymentUseCase.getPaymentById(id);
        if (!Caller.from(userIdHeader, rolesHeader).canAccess(payment.getUserId())) {
            throw new ResourceNotFoundException("Payment not found with id: " + id);
        }
        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(
            @RequestHeader(value = Caller.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = Caller.USER_ROLES_HEADER, required = false) String rolesHeader,
            @PathVariable UUID orderId) {
        Caller caller = Caller.from(userIdHeader, rolesHeader);
        // Other users' payments are filtered out rather than rejected: the list is simply empty for them.
        List<PaymentResponse> responses = paymentUseCase.getPaymentsByOrderId(orderId).stream()
                .filter(payment -> caller.canAccess(payment.getUserId()))
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

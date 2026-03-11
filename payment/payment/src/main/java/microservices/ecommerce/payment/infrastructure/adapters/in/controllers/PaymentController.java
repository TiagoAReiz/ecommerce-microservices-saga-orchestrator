package microservices.ecommerce.payment.infrastructure.adapters.in.controllers;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.payment.application.mappers.PaymentMapper;
import microservices.ecommerce.payment.application.ports.in.usecases.PaymentUseCase;
import microservices.ecommerce.payment.core.entities.Payment;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentRequest;
import microservices.ecommerce.payment.infrastructure.adapters.in.controllers.dtos.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentUseCase paymentUseCase;
    private final PaymentMapper paymentMapper;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentUseCase.processPayment(request);
        return new ResponseEntity<>(paymentMapper.toResponse(payment), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
        Payment payment = paymentUseCase.getPaymentById(id);
        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(@PathVariable UUID orderId) {
        List<PaymentResponse> responses = paymentUseCase.getPaymentsByOrderId(orderId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

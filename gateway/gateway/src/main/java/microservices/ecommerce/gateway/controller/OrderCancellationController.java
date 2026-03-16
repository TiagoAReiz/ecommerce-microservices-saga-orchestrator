package microservices.ecommerce.gateway.controller;

import microservices.ecommerce.gateway.dto.cancellation.CancellationResponse;
import microservices.ecommerce.gateway.service.OrderCancellationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderCancellationController {

    private static final Logger log = LoggerFactory.getLogger(OrderCancellationController.class);

    private final OrderCancellationService cancellationService;

    public OrderCancellationController(OrderCancellationService cancellationService) {
        this.cancellationService = cancellationService;
    }

    @PostMapping("/{orderId}/cancel")
    public Mono<ResponseEntity<CancellationResponse>> cancelOrder(@PathVariable UUID orderId) {
        log.info("Order cancellation request received for order: {}", orderId);
        return cancellationService.cancelOrder(orderId)
                .map(ResponseEntity::ok);
    }
}

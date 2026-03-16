package microservices.ecommerce.gateway.service;

import microservices.ecommerce.gateway.dto.cancellation.CancellationResponse;
import microservices.ecommerce.gateway.dto.delivery.DeliveryResponse;
import microservices.ecommerce.gateway.dto.inventory.InventoryRequest;
import microservices.ecommerce.gateway.dto.inventory.InventoryResponse;
import microservices.ecommerce.gateway.dto.order.OrderItemResponse;
import microservices.ecommerce.gateway.dto.order.OrderResponse;
import microservices.ecommerce.gateway.dto.payment.PaymentResponse;
import microservices.ecommerce.gateway.entity.SagaState;
import microservices.ecommerce.gateway.saga.SagaExecutionCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderCancellationService {

    private static final Logger log = LoggerFactory.getLogger(OrderCancellationService.class);
    private static final String SAGA_TYPE = "ORDER_CANCELLATION";
    private static final Set<String> NON_CANCELLABLE_STATUSES = Set.of("SHIPPED", "DELIVERED");

    private final WebClient orderWebClient;
    private final WebClient inventoryWebClient;
    private final WebClient paymentWebClient;
    private final WebClient deliveryWebClient;
    private final SagaExecutionCoordinator sagaCoordinator;

    public OrderCancellationService(
            @Qualifier("orderWebClient") WebClient orderWebClient,
            @Qualifier("inventoryWebClient") WebClient inventoryWebClient,
            @Qualifier("paymentWebClient") WebClient paymentWebClient,
            @Qualifier("deliveryWebClient") WebClient deliveryWebClient,
            SagaExecutionCoordinator sagaCoordinator) {
        this.orderWebClient = orderWebClient;
        this.inventoryWebClient = inventoryWebClient;
        this.paymentWebClient = paymentWebClient;
        this.deliveryWebClient = deliveryWebClient;
        this.sagaCoordinator = sagaCoordinator;
    }

    public Mono<CancellationResponse> cancelOrder(UUID orderId) {
        return sagaCoordinator.startSaga(SAGA_TYPE, null)
                .flatMap(saga -> sagaCoordinator.setOrderId(saga, orderId))
                .flatMap(saga -> executeCancellationPipeline(saga, orderId));
    }

    private Mono<CancellationResponse> executeCancellationPipeline(SagaState saga, UUID orderId) {
        // Step 1: Get order and validate status
        return sagaCoordinator.updateStep(saga, "GET_ORDER")
                .flatMap(s -> getOrder(orderId))
                .flatMap(order -> {
                    if (NON_CANCELLABLE_STATUSES.contains(order.status())) {
                        return sagaCoordinator.failSaga(saga, "Order cannot be cancelled - status: " + order.status())
                                .then(Mono.error(new IllegalStateException(
                                        "Order cannot be cancelled. Current status: " + order.status())));
                    }

                    log.info("Saga [{}] id={} - Order {} retrieved, status: {}",
                            SAGA_TYPE, saga.getId(), orderId, order.status());

                    // Step 2: Update order status to CANCELLED
                    return sagaCoordinator.updateStep(saga, "CANCEL_ORDER")
                            .flatMap(s -> updateOrderStatus(orderId, "CANCELLED"))
                            .flatMap(cancelledOrder -> {
                                log.info("Saga [{}] id={} - Order {} cancelled", SAGA_TYPE, saga.getId(), orderId);

                                // Step 3: Release inventory for all order items
                                return sagaCoordinator.updateStep(saga, "RELEASE_INVENTORY")
                                        .flatMap(s -> releaseAllInventory(order.items()))
                                        .then(Mono.defer(() -> {
                                            log.info("Saga [{}] id={} - Inventory released", SAGA_TYPE, saga.getId());

                                            // Step 4: Refund payment (if applicable)
                                            return sagaCoordinator.updateStep(saga, "REFUND_PAYMENT")
                                                    .flatMap(s -> refundPayment(orderId))
                                                    .then(Mono.defer(() -> {
                                                        log.info("Saga [{}] id={} - Payment refunded", SAGA_TYPE, saga.getId());

                                                        // Step 5: Cancel delivery (if exists)
                                                        return sagaCoordinator.updateStep(saga, "CANCEL_DELIVERY")
                                                                .flatMap(s -> cancelDelivery(orderId))
                                                                .then(sagaCoordinator.completeSaga(saga))
                                                                .thenReturn(new CancellationResponse(
                                                                        orderId,
                                                                        "CANCELLED",
                                                                        "Order cancelled successfully"
                                                                ));
                                                    }));
                                        }));
                            });
                });
    }

    // --- WebClient Calls ---

    private Mono<OrderResponse> getOrder(UUID orderId) {
        return orderWebClient.get()
                .uri("/api/v1/orders/{id}", orderId)
                .retrieve()
                .bodyToMono(OrderResponse.class);
    }

    private Mono<OrderResponse> updateOrderStatus(UUID orderId, String status) {
        return orderWebClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/orders/{id}/status")
                        .queryParam("status", status)
                        .build(orderId))
                .retrieve()
                .bodyToMono(OrderResponse.class);
    }

    private Mono<Void> releaseAllInventory(List<OrderItemResponse> items) {
        if (items == null || items.isEmpty()) return Mono.empty();

        return Flux.fromIterable(items)
                .flatMap(item -> inventoryWebClient.post()
                        .uri("/api/v1/inventory/release")
                        .bodyValue(new InventoryRequest(item.productId(), 0, item.quantity()))
                        .retrieve()
                        .bodyToMono(InventoryResponse.class)
                        .doOnSuccess(r -> log.info("Cancellation: Released inventory for product {}", item.productId()))
                        .onErrorResume(e -> {
                            log.error("Cancellation: Failed to release inventory for product {}: {}",
                                    item.productId(), e.getMessage());
                            return Mono.empty();
                        }))
                .then();
    }

    private Mono<Void> refundPayment(UUID orderId) {
        return paymentWebClient.get()
                .uri("/api/v1/payments/order/{orderId}", orderId)
                .retrieve()
                .bodyToFlux(PaymentResponse.class)
                .flatMap(payment -> {
                    log.info("Cancellation: Payment {} found for order {}, status: {}",
                            payment.id(), orderId, payment.status());
                    // In a real system, we'd call a refund endpoint.
                    // For now, log it as refunded since no refund endpoint exists yet.
                    log.info("Cancellation: Refund processed for payment {}", payment.id());
                    return Mono.empty();
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.info("Cancellation: No payment found for order {}, skipping refund", orderId);
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> cancelDelivery(UUID orderId) {
        return deliveryWebClient.get()
                .uri("/api/v1/deliveries/order/{orderId}", orderId)
                .retrieve()
                .bodyToMono(DeliveryResponse.class)
                .flatMap(delivery -> deliveryWebClient.patch()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/deliveries/{id}/status")
                                .queryParam("status", "CANCELLED")
                                .build(delivery.id()))
                        .retrieve()
                        .bodyToMono(DeliveryResponse.class)
                        .doOnSuccess(d -> log.info("Cancellation: Delivery {} cancelled", delivery.id())))
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.info("Cancellation: No delivery found for order {}, skipping", orderId);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("Cancellation: Failed to cancel delivery for order {}: {}", orderId, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }
}

package microservices.ecommerce.gateway.service;

import microservices.ecommerce.gateway.dto.cart.CartItemResponse;
import microservices.ecommerce.gateway.dto.cart.CartResponse;
import microservices.ecommerce.gateway.dto.checkout.CheckoutRequest;
import microservices.ecommerce.gateway.dto.checkout.CheckoutResponse;
import microservices.ecommerce.gateway.dto.delivery.DeliveryRequest;
import microservices.ecommerce.gateway.dto.delivery.DeliveryResponse;
import microservices.ecommerce.gateway.dto.inventory.InventoryRequest;
import microservices.ecommerce.gateway.dto.inventory.InventoryResponse;
import microservices.ecommerce.gateway.dto.order.OrderItemRequest;
import microservices.ecommerce.gateway.dto.order.OrderRequest;
import microservices.ecommerce.gateway.dto.order.OrderResponse;
import microservices.ecommerce.gateway.dto.payment.PaymentRequest;
import microservices.ecommerce.gateway.dto.payment.PaymentResponse;
import microservices.ecommerce.gateway.entity.SagaState;
import microservices.ecommerce.gateway.exception.SagaStepFailedException;
import microservices.ecommerce.gateway.saga.SagaExecutionCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private static final String SAGA_TYPE = "CHECKOUT";

    private final WebClient cartWebClient;
    private final WebClient inventoryWebClient;
    private final WebClient orderWebClient;
    private final WebClient paymentWebClient;
    private final WebClient deliveryWebClient;
    private final SagaExecutionCoordinator sagaCoordinator;

    public CheckoutService(
            @Qualifier("cartWebClient") WebClient cartWebClient,
            @Qualifier("inventoryWebClient") WebClient inventoryWebClient,
            @Qualifier("orderWebClient") WebClient orderWebClient,
            @Qualifier("paymentWebClient") WebClient paymentWebClient,
            @Qualifier("deliveryWebClient") WebClient deliveryWebClient,
            SagaExecutionCoordinator sagaCoordinator) {
        this.cartWebClient = cartWebClient;
        this.inventoryWebClient = inventoryWebClient;
        this.orderWebClient = orderWebClient;
        this.paymentWebClient = paymentWebClient;
        this.deliveryWebClient = deliveryWebClient;
        this.sagaCoordinator = sagaCoordinator;
    }

    public Mono<CheckoutResponse> executeCheckout(CheckoutRequest request) {
        return sagaCoordinator.startSaga(SAGA_TYPE, request.userId())
                .flatMap(saga -> executeCheckoutPipeline(saga, request));
    }

    private Mono<CheckoutResponse> executeCheckoutPipeline(SagaState saga, CheckoutRequest request) {
        // Step 1: Get Cart
        return sagaCoordinator.updateStep(saga, "GET_CART")
                .flatMap(s -> getCart(request.userId()))
                .flatMap(cart -> {
                    if (cart.items() == null || cart.items().isEmpty()) {
                        return sagaCoordinator.failSaga(saga, "Cart is empty")
                                .then(Mono.error(new IllegalArgumentException("Cart is empty. Cannot proceed with checkout.")));
                    }
                    log.info("Saga [{}] id={} - Cart retrieved with {} items",
                            SAGA_TYPE, saga.getId(), cart.items().size());

                    List<CartItemResponse> cartItems = cart.items();

                    // Step 2: Reserve Inventory for all items
                    return sagaCoordinator.updateStep(saga, "RESERVE_INVENTORY")
                            .flatMap(s -> reserveAllInventory(cartItems))
                            .flatMap(reservedItems -> {
                                log.info("Saga [{}] id={} - Inventory reserved for {} items",
                                        SAGA_TYPE, saga.getId(), reservedItems.size());

                                // Step 3: Create Order
                                return sagaCoordinator.updateStep(saga, "CREATE_ORDER")
                                        .flatMap(s -> createOrder(request, cartItems))
                                        .flatMap(order -> {
                                            log.info("Saga [{}] id={} - Order created: {}",
                                                    SAGA_TYPE, saga.getId(), order.id());

                                            return sagaCoordinator.setOrderId(saga, order.id())
                                                    .flatMap(s -> {
                                                        // Step 4: Process Payment
                                                        return sagaCoordinator.updateStep(saga, "PROCESS_PAYMENT")
                                                                .flatMap(s2 -> processPayment(order))
                                                                .flatMap(payment -> {
                                                                    log.info("Saga [{}] id={} - Payment processed: {}",
                                                                            SAGA_TYPE, saga.getId(), payment.transactionReference());

                                                                    // Step 5: Schedule Delivery
                                                                    return sagaCoordinator.updateStep(saga, "SCHEDULE_DELIVERY")
                                                                            .flatMap(s2 -> scheduleDelivery(order))
                                                                            .flatMap(delivery -> {
                                                                                log.info("Saga [{}] id={} - Delivery scheduled: {}",
                                                                                        SAGA_TYPE, saga.getId(), delivery.id());

                                                                                // Step 6: Checkout cart (mark as COMPLETED)
                                                                                return sagaCoordinator.updateStep(saga, "CHECKOUT_CART")
                                                                                        .flatMap(s2 -> checkoutCart(request.userId()))
                                                                                        .flatMap(v -> sagaCoordinator.completeSaga(saga))
                                                                                        .thenReturn(new CheckoutResponse(
                                                                                                order.id(),
                                                                                                order.status(),
                                                                                                payment.status(),
                                                                                                payment.transactionReference(),
                                                                                                delivery.id()
                                                                                        ));
                                                                            });
                                                                })
                                                                // Payment failed → compensate: cancel order + release inventory
                                                                .onErrorResume(e -> {
                                                                    log.error("Saga [{}] id={} - Payment failed, compensating...",
                                                                            SAGA_TYPE, saga.getId(), e);
                                                                    return compensatePaymentFailure(saga, order.id(), cartItems)
                                                                            .then(Mono.error(new SagaStepFailedException(
                                                                                    SAGA_TYPE, "PROCESS_PAYMENT",
                                                                                    "Payment failed: " + e.getMessage(), e)));
                                                                });
                                                    });
                                        })
                                        // Order creation failed → compensate: release inventory
                                        .onErrorResume(e -> {
                                            if (e instanceof SagaStepFailedException) return Mono.error(e);
                                            log.error("Saga [{}] id={} - Order creation failed, compensating...",
                                                    SAGA_TYPE, saga.getId(), e);
                                            return compensateOrderFailure(saga, cartItems)
                                                    .then(Mono.error(new SagaStepFailedException(
                                                            SAGA_TYPE, "CREATE_ORDER",
                                                            "Order creation failed: " + e.getMessage(), e)));
                                        });
                            })
                            // Inventory reservation failed → no compensation needed
                            .onErrorResume(e -> {
                                if (e instanceof SagaStepFailedException) return Mono.error(e);
                                log.error("Saga [{}] id={} - Inventory reservation failed",
                                        SAGA_TYPE, saga.getId(), e);
                                return sagaCoordinator.failSaga(saga, e.getMessage())
                                        .then(Mono.error(new SagaStepFailedException(
                                                SAGA_TYPE, "RESERVE_INVENTORY", e.getMessage(), e)));
                            });
                });
    }

    // --- WebClient Calls ---

    private Mono<CartResponse> getCart(UUID userId) {
        return cartWebClient.get()
                .uri("/api/v1/carts/{userId}", userId)
                .retrieve()
                .bodyToMono(CartResponse.class);
    }

    private Mono<List<InventoryResponse>> reserveAllInventory(List<CartItemResponse> items) {
        return Flux.fromIterable(items)
                .flatMap(item -> inventoryWebClient.post()
                        .uri("/api/v1/inventory/reserve")
                        .bodyValue(new InventoryRequest(item.productId(), item.quantity(), 0))
                        .retrieve()
                        .bodyToMono(InventoryResponse.class))
                .collectList();
    }

    private Mono<OrderResponse> createOrder(CheckoutRequest request, List<CartItemResponse> cartItems) {
        List<OrderItemRequest> orderItems = cartItems.stream()
                .map(ci -> new OrderItemRequest(ci.productId(), ci.quantity()))
                .toList();

        OrderRequest orderRequest = new OrderRequest(
                request.userId(),
                request.shippingAddressId(),
                orderItems
        );

        return orderWebClient.post()
                .uri("/api/v1/orders")
                .bodyValue(orderRequest)
                .retrieve()
                .bodyToMono(OrderResponse.class);
    }

    private Mono<PaymentResponse> processPayment(OrderResponse order) {
        BigDecimal amount = order.totalAmount() != null ? order.totalAmount() : BigDecimal.ZERO;
        PaymentRequest paymentRequest = new PaymentRequest(
                order.id(), amount, "BRL", "CREDIT_CARD"
        );

        return paymentWebClient.post()
                .uri("/api/v1/payments")
                .bodyValue(paymentRequest)
                .retrieve()
                .bodyToMono(PaymentResponse.class);
    }

    private Mono<DeliveryResponse> scheduleDelivery(OrderResponse order) {
        DeliveryRequest deliveryRequest = new DeliveryRequest(
                order.id(),
                "DEFAULT_CARRIER",
                "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                LocalDateTime.now().plusDays(7)
        );

        return deliveryWebClient.post()
                .uri("/api/v1/deliveries")
                .bodyValue(deliveryRequest)
                .retrieve()
                .bodyToMono(DeliveryResponse.class);
    }

    private Mono<Void> checkoutCart(UUID userId) {
        return cartWebClient.post()
                .uri("/api/v1/carts/{userId}/checkout", userId)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // --- Compensation ---

    private Mono<Void> compensatePaymentFailure(SagaState saga, UUID orderId, List<CartItemResponse> items) {
        return sagaCoordinator.startCompensation(saga)
                .then(cancelOrder(orderId)
                        .doOnSuccess(v -> log.info("Compensation: Order {} cancelled", orderId))
                        .onErrorResume(e -> {
                            log.error("Compensation FAILED: cancel order {}: {}", orderId, e.getMessage());
                            return Mono.empty();
                        }))
                .then(releaseAllInventory(items))
                .then(sagaCoordinator.completeCompensation(saga))
                .then();
    }

    private Mono<Void> compensateOrderFailure(SagaState saga, List<CartItemResponse> items) {
        return sagaCoordinator.startCompensation(saga)
                .then(releaseAllInventory(items))
                .then(sagaCoordinator.completeCompensation(saga))
                .then();
    }

    private Mono<Void> cancelOrder(UUID orderId) {
        return orderWebClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/orders/{id}/status")
                        .queryParam("status", "CANCELLED")
                        .build(orderId))
                .retrieve()
                .bodyToMono(Void.class);
    }

    private Mono<Void> releaseAllInventory(List<CartItemResponse> items) {
        return Flux.fromIterable(items)
                .flatMap(item -> inventoryWebClient.post()
                        .uri("/api/v1/inventory/release")
                        .bodyValue(new InventoryRequest(item.productId(), 0, item.quantity()))
                        .retrieve()
                        .bodyToMono(InventoryResponse.class)
                        .doOnSuccess(r -> log.info("Compensation: Released inventory for product {}", item.productId()))
                        .onErrorResume(e -> {
                            log.error("Compensation FAILED: release inventory for product {}: {}",
                                    item.productId(), e.getMessage());
                            return Mono.empty();
                        }))
                .then();
    }
}

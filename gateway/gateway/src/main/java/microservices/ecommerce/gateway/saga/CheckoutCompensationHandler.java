package microservices.ecommerce.gateway.saga;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import microservices.ecommerce.gateway.dto.cart.CartItemResponse;
import microservices.ecommerce.gateway.dto.inventory.InventoryRequest;
import microservices.ecommerce.gateway.dto.inventory.InventoryResponse;
import microservices.ecommerce.gateway.entity.SagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Handles compensation for stuck CHECKOUT sagas.
 *
 * Compensation is best-effort: each step is attempted and errors are logged
 * but do not abort the remaining compensation steps.
 */
@Component
public class CheckoutCompensationHandler {

    private static final Logger log = LoggerFactory.getLogger(CheckoutCompensationHandler.class);

    private static final Set<String> STEPS_NEEDING_INVENTORY_RELEASE =
            Set.of("RESERVE_INVENTORY", "CREATE_ORDER", "PROCESS_PAYMENT", "SCHEDULE_DELIVERY", "CHECKOUT_CART");

    private static final Set<String> STEPS_NEEDING_ORDER_CANCEL =
            Set.of("CREATE_ORDER", "PROCESS_PAYMENT", "SCHEDULE_DELIVERY", "CHECKOUT_CART");

    private static final Set<String> STEPS_NEEDING_PAYMENT_REFUND =
            Set.of("PROCESS_PAYMENT", "SCHEDULE_DELIVERY", "CHECKOUT_CART");

    private static final Set<String> STEPS_NEEDING_DELIVERY_CANCEL =
            Set.of("SCHEDULE_DELIVERY", "CHECKOUT_CART");

    private final WebClient inventoryWebClient;
    private final WebClient orderWebClient;
    private final WebClient paymentWebClient;
    private final WebClient deliveryWebClient;
    private final ObjectMapper objectMapper;

    public CheckoutCompensationHandler(
            @Qualifier("inventoryWebClient") WebClient inventoryWebClient,
            @Qualifier("orderWebClient") WebClient orderWebClient,
            @Qualifier("paymentWebClient") WebClient paymentWebClient,
            @Qualifier("deliveryWebClient") WebClient deliveryWebClient,
            ObjectMapper objectMapper) {
        this.inventoryWebClient = inventoryWebClient;
        this.orderWebClient = orderWebClient;
        this.paymentWebClient = paymentWebClient;
        this.deliveryWebClient = deliveryWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes compensation actions for a stuck CHECKOUT saga based on its current step.
     * Each action is best-effort: failures are logged and do not stop subsequent steps.
     */
    public Mono<Void> compensate(SagaState saga) {
        String step = saga.getCurrentStep();
        UUID orderId = saga.getOrderId();
        List<CartItemResponse> cartItems = parseCartItems(saga.getPayload());

        log.warn("CheckoutCompensation: starting for saga id={} stuck at step={}", saga.getId(), step);

        Mono<Void> chain = Mono.empty();

        if (STEPS_NEEDING_DELIVERY_CANCEL.contains(step) && orderId != null) {
            chain = chain.then(cancelDelivery(saga.getId(), orderId));
        }

        if (STEPS_NEEDING_PAYMENT_REFUND.contains(step) && orderId != null) {
            chain = chain.then(refundPayment(saga.getId(), orderId));
        }

        if (STEPS_NEEDING_ORDER_CANCEL.contains(step) && orderId != null) {
            chain = chain.then(cancelOrder(saga.getId(), orderId));
        }

        if (STEPS_NEEDING_INVENTORY_RELEASE.contains(step) && !cartItems.isEmpty()) {
            chain = chain.then(releaseInventory(saga.getId(), cartItems));
        }

        return chain.doOnSuccess(v ->
                log.info("CheckoutCompensation: completed for saga id={}", saga.getId()));
    }

    private Mono<Void> cancelDelivery(UUID sagaId, UUID orderId) {
        return deliveryWebClient.get()
                .uri("/api/v1/deliveries/order/{orderId}", orderId)
                .retrieve()
                .bodyToMono(microservices.ecommerce.gateway.dto.delivery.DeliveryResponse.class)
                .flatMap(delivery -> deliveryWebClient.patch()
                        .uri(b -> b.path("/api/v1/deliveries/{id}/status")
                                .queryParam("status", "CANCELLED")
                                .build(delivery.id()))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .doOnSuccess(v -> log.info("CheckoutCompensation [{}]: delivery {} cancelled", sagaId, delivery.id())))
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.info("CheckoutCompensation [{}]: no delivery found for order {}, skipping", sagaId, orderId);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("CheckoutCompensation [{}]: failed to cancel delivery for order {}: {}", sagaId, orderId, e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> refundPayment(UUID sagaId, UUID orderId) {
        return paymentWebClient.get()
                .uri("/api/v1/payments/order/{orderId}", orderId)
                .retrieve()
                .bodyToFlux(microservices.ecommerce.gateway.dto.payment.PaymentResponse.class)
                .flatMap(payment -> {
                    log.info("CheckoutCompensation [{}]: refunding payment {} for order {}", sagaId, payment.id(), orderId);
                    // Placeholder: call refund endpoint when implemented
                    return Mono.empty();
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.info("CheckoutCompensation [{}]: no payment found for order {}, skipping refund", sagaId, orderId);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("CheckoutCompensation [{}]: failed to refund payment for order {}: {}", sagaId, orderId, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> cancelOrder(UUID sagaId, UUID orderId) {
        return orderWebClient.patch()
                .uri(b -> b.path("/api/v1/orders/{id}/status")
                        .queryParam("status", "CANCELLED")
                        .build(orderId))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("CheckoutCompensation [{}]: order {} cancelled", sagaId, orderId))
                .onErrorResume(e -> {
                    log.error("CheckoutCompensation [{}]: failed to cancel order {}: {}", sagaId, orderId, e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> releaseInventory(UUID sagaId, List<CartItemResponse> items) {
        return Flux.fromIterable(items)
                .flatMap(item -> inventoryWebClient.post()
                        .uri("/api/v1/inventory/release")
                        .bodyValue(new InventoryRequest(item.productId(), 0, item.quantity()))
                        .retrieve()
                        .bodyToMono(InventoryResponse.class)
                        .doOnSuccess(r -> log.info("CheckoutCompensation [{}]: released inventory for product {}", sagaId, item.productId()))
                        .onErrorResume(e -> {
                            log.error("CheckoutCompensation [{}]: failed to release inventory for product {}: {}",
                                    sagaId, item.productId(), e.getMessage());
                            return Mono.empty();
                        }))
                .then();
    }

    private List<CartItemResponse> parseCartItems(String payload) {
        if (payload == null || payload.isBlank() || payload.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<List<CartItemResponse>>() {});
        } catch (Exception e) {
            log.warn("CheckoutCompensation: failed to parse cartItems from payload: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}

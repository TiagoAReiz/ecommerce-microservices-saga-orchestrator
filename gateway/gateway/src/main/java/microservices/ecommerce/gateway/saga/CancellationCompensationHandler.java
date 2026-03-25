package microservices.ecommerce.gateway.saga;

import microservices.ecommerce.gateway.entity.SagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handles compensation for stuck ORDER_CANCELLATION sagas.
 *
 * Most steps of a cancellation saga are not safely reversible (e.g., inventory
 * already released cannot be re-reserved without knowing exact state). The handler
 * attempts a best-effort reversal where possible and logs warnings for irreversible cases.
 */
@Component
public class CancellationCompensationHandler {

    private static final Logger log = LoggerFactory.getLogger(CancellationCompensationHandler.class);

    private final WebClient orderWebClient;

    public CancellationCompensationHandler(
            @Qualifier("orderWebClient") WebClient orderWebClient) {
        this.orderWebClient = orderWebClient;
    }

    /**
     * Attempts compensation for a stuck ORDER_CANCELLATION saga.
     *
     * <ul>
     *   <li>GET_ORDER: nothing was modified, safe to just fail.</li>
     *   <li>CANCEL_ORDER: order status may be CANCELLED prematurely; try to revert to PENDING.</li>
     *   <li>RELEASE_INVENTORY / REFUND_PAYMENT / CANCEL_DELIVERY: irreversible; log and fail.</li>
     * </ul>
     */
    public Mono<Void> compensate(SagaState saga) {
        String step = saga.getCurrentStep();
        UUID orderId = saga.getOrderId();

        log.warn("CancellationCompensation: starting for saga id={} stuck at step={}", saga.getId(), step);

        return switch (step) {
            case "INITIATED", "GET_ORDER" -> {
                log.info("CancellationCompensation [{}]: step {} has no side effects, marking failed", saga.getId(), step);
                yield Mono.empty();
            }
            case "CANCEL_ORDER" -> revertOrderToPending(saga.getId(), orderId);
            case "RELEASE_INVENTORY", "REFUND_PAYMENT", "CANCEL_DELIVERY" -> {
                log.warn("CancellationCompensation [{}]: step {} is irreversible. " +
                                "Manual intervention may be required for order {}.",
                        saga.getId(), step, orderId);
                yield Mono.empty();
            }
            default -> {
                log.warn("CancellationCompensation [{}]: unknown step '{}', no compensation applied", saga.getId(), step);
                yield Mono.empty();
            }
        };
    }

    private Mono<Void> revertOrderToPending(UUID sagaId, UUID orderId) {
        if (orderId == null) {
            log.warn("CancellationCompensation [{}]: cannot revert order status — orderId is null", sagaId);
            return Mono.empty();
        }
        return orderWebClient.patch()
                .uri(b -> b.path("/api/v1/orders/{id}/status")
                        .queryParam("status", "PENDING")
                        .build(orderId))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("CancellationCompensation [{}]: order {} reverted to PENDING", sagaId, orderId))
                .onErrorResume(e -> {
                    log.error("CancellationCompensation [{}]: failed to revert order {} to PENDING: {}",
                            sagaId, orderId, e.getMessage());
                    return Mono.empty();
                });
    }
}

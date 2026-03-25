package microservices.ecommerce.gateway.saga;

import microservices.ecommerce.gateway.entity.SagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Routes a stuck saga to the appropriate compensation handler based on its sagaType.
 */
@Component
public class SagaCompensationRouter {

    private static final Logger log = LoggerFactory.getLogger(SagaCompensationRouter.class);

    private final CheckoutCompensationHandler checkoutHandler;
    private final CancellationCompensationHandler cancellationHandler;

    public SagaCompensationRouter(CheckoutCompensationHandler checkoutHandler,
                                  CancellationCompensationHandler cancellationHandler) {
        this.checkoutHandler = checkoutHandler;
        this.cancellationHandler = cancellationHandler;
    }

    public Mono<Void> compensate(SagaState saga) {
        return switch (saga.getSagaType()) {
            case "CHECKOUT" -> checkoutHandler.compensate(saga);
            case "ORDER_CANCELLATION" -> cancellationHandler.compensate(saga);
            default -> {
                log.warn("SagaCompensationRouter: unknown sagaType '{}' for saga id={}. No compensation applied.",
                        saga.getSagaType(), saga.getId());
                yield Mono.empty();
            }
        };
    }
}

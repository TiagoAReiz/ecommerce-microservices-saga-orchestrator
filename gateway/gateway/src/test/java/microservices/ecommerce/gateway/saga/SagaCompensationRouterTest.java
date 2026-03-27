package microservices.ecommerce.gateway.saga;

import microservices.ecommerce.gateway.entity.SagaState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaCompensationRouterTest {

    @Mock
    private CheckoutCompensationHandler checkoutHandler;

    @Mock
    private CancellationCompensationHandler cancellationHandler;

    private SagaCompensationRouter router;

    @BeforeEach
    void setUp() {
        router = new SagaCompensationRouter(checkoutHandler, cancellationHandler);
    }

    @Test
    void compensate_checkoutType_delegatesToCheckoutHandler() {
        SagaState saga = buildSaga("CHECKOUT", "PROCESS_PAYMENT");
        when(checkoutHandler.compensate(any())).thenReturn(Mono.empty());

        StepVerifier.create(router.compensate(saga))
                .verifyComplete();

        verify(checkoutHandler).compensate(saga);
        verifyNoInteractions(cancellationHandler);
    }

    @Test
    void compensate_orderCancellationType_delegatesToCancellationHandler() {
        SagaState saga = buildSaga("ORDER_CANCELLATION", "CANCEL_ORDER");
        when(cancellationHandler.compensate(any())).thenReturn(Mono.empty());

        StepVerifier.create(router.compensate(saga))
                .verifyComplete();

        verify(cancellationHandler).compensate(saga);
        verifyNoInteractions(checkoutHandler);
    }

    @Test
    void compensate_unknownType_completesWithoutCallingHandlers() {
        SagaState saga = buildSaga("UNKNOWN_TYPE", "SOME_STEP");

        StepVerifier.create(router.compensate(saga))
                .verifyComplete();

        verifyNoInteractions(checkoutHandler, cancellationHandler);
    }

    private SagaState buildSaga(String type, String step) {
        return SagaState.builder()
                .id(UUID.randomUUID())
                .sagaType(type)
                .currentStep(step)
                .status(SagaState.STATUS_IN_PROGRESS)
                .build();
    }
}

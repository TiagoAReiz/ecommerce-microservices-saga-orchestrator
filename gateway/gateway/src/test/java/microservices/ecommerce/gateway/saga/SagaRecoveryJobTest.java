package microservices.ecommerce.gateway.saga;

import microservices.ecommerce.gateway.entity.SagaState;
import microservices.ecommerce.gateway.repository.SagaStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaRecoveryJobTest {

    @Mock
    private SagaStateRepository repository;

    @Mock
    private SagaExecutionCoordinator sagaCoordinator;

    @Mock
    private SagaCompensationRouter compensationRouter;

    private SagaRecoveryJob recoveryJob;

    private static final int STUCK_THRESHOLD = 5;
    private static final int MAX_RETRIES = 3;

    @BeforeEach
    void setUp() {
        recoveryJob = new SagaRecoveryJob(repository, sagaCoordinator, compensationRouter,
                STUCK_THRESHOLD, MAX_RETRIES);
    }

    @Test
    void recoverStuckSagas_belowMaxRetries_compensatesAndCompletes() {
        SagaState stuckSaga = buildStuckSaga("CHECKOUT", "PROCESS_PAYMENT", 0);

        when(repository.findStuckSagas(any(LocalDateTime.class))).thenReturn(Flux.just(stuckSaga));
        when(sagaCoordinator.startCompensation(any())).thenReturn(Mono.just(stuckSaga));
        when(compensationRouter.compensate(any())).thenReturn(Mono.empty());
        when(sagaCoordinator.completeCompensation(any())).thenReturn(Mono.just(stuckSaga));

        recoveryJob.recoverStuckSagas();

        verify(sagaCoordinator).startCompensation(stuckSaga);
        verify(compensationRouter).compensate(stuckSaga);
        verify(sagaCoordinator).completeCompensation(stuckSaga);
        verify(sagaCoordinator, never()).failSaga(any(), anyString());
    }

    @Test
    void recoverStuckSagas_maxRetriesExceeded_marksFailedWithoutCompensation() {
        SagaState stuckSaga = buildStuckSaga("CHECKOUT", "CREATE_ORDER", MAX_RETRIES);

        when(repository.findStuckSagas(any(LocalDateTime.class))).thenReturn(Flux.just(stuckSaga));
        when(sagaCoordinator.failSaga(any(), anyString())).thenReturn(Mono.just(stuckSaga));

        recoveryJob.recoverStuckSagas();

        verify(sagaCoordinator).failSaga(eq(stuckSaga), anyString());
        verify(compensationRouter, never()).compensate(any());
        verify(sagaCoordinator, never()).startCompensation(any());
    }

    @Test
    void recoverStuckSagas_compensationFails_marksAsFailedViaCoordinator() {
        SagaState stuckSaga = buildStuckSaga("ORDER_CANCELLATION", "CANCEL_ORDER", 1);

        when(repository.findStuckSagas(any(LocalDateTime.class))).thenReturn(Flux.just(stuckSaga));
        when(sagaCoordinator.startCompensation(any())).thenReturn(Mono.just(stuckSaga));
        when(compensationRouter.compensate(any())).thenReturn(Mono.error(new RuntimeException("network error")));
        when(sagaCoordinator.failSaga(any(), anyString())).thenReturn(Mono.just(stuckSaga));

        recoveryJob.recoverStuckSagas();

        verify(sagaCoordinator).startCompensation(stuckSaga);
        verify(compensationRouter).compensate(stuckSaga);
        verify(sagaCoordinator).failSaga(eq(stuckSaga), anyString());
        verify(sagaCoordinator, never()).completeCompensation(any());
    }

    @Test
    void recoverStuckSagas_noStuckSagas_doesNothing() {
        when(repository.findStuckSagas(any(LocalDateTime.class))).thenReturn(Flux.empty());

        recoveryJob.recoverStuckSagas();

        verifyNoInteractions(sagaCoordinator, compensationRouter);
    }

    @Test
    void recoverStuckSagas_incrementsRetryCountBeforeCompensation() {
        SagaState stuckSaga = buildStuckSaga("CHECKOUT", "SCHEDULE_DELIVERY", 1);

        when(repository.findStuckSagas(any(LocalDateTime.class))).thenReturn(Flux.just(stuckSaga));
        when(sagaCoordinator.startCompensation(any())).thenReturn(Mono.just(stuckSaga));
        when(compensationRouter.compensate(any())).thenReturn(Mono.empty());
        when(sagaCoordinator.completeCompensation(any())).thenReturn(Mono.just(stuckSaga));

        recoveryJob.recoverStuckSagas();

        // retryCount should have been incremented to 2 before startCompensation was called
        verify(sagaCoordinator).startCompensation(argThat(s -> s.getRetryCount() == 2));
    }

    private SagaState buildStuckSaga(String type, String step, int retryCount) {
        SagaState saga = SagaState.builder()
                .id(UUID.randomUUID())
                .sagaType(type)
                .currentStep(step)
                .status(SagaState.STATUS_IN_PROGRESS)
                .updatedAt(LocalDateTime.now().minusMinutes(STUCK_THRESHOLD + 1))
                .build();
        saga.setRetryCount(retryCount);
        return saga;
    }
}

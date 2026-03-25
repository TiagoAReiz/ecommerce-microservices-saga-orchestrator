package microservices.ecommerce.gateway.saga;

import microservices.ecommerce.gateway.entity.SagaState;
import microservices.ecommerce.gateway.repository.SagaStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SagaExecutionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(SagaExecutionCoordinator.class);

    private final SagaStateRepository repository;

    public SagaExecutionCoordinator(SagaStateRepository repository) {
        this.repository = repository;
    }

    public Mono<SagaState> startSaga(String sagaType, UUID userId) {
        SagaState state = SagaState.builder()
                .id(UUID.randomUUID())
                .sagaType(sagaType)
                .currentStep("INITIATED")
                .status(SagaState.STATUS_STARTED)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("Starting saga [{}] id={} for user={}", sagaType, state.getId(), userId);
        return repository.save(state);
    }

    public Mono<SagaState> updateStep(SagaState state, String step) {
        state.setCurrentStep(step);
        state.setStatus(SagaState.STATUS_IN_PROGRESS);
        state.setUpdatedAt(LocalDateTime.now());
        log.info("Saga [{}] id={} transitioning to step [{}]", state.getSagaType(), state.getId(), step);
        return repository.save(state);
    }

    public Mono<SagaState> completeSaga(SagaState state) {
        state.setStatus(SagaState.STATUS_COMPLETED);
        state.setUpdatedAt(LocalDateTime.now());
        log.info("Saga [{}] id={} COMPLETED successfully", state.getSagaType(), state.getId());
        return repository.save(state);
    }

    public Mono<SagaState> failSaga(SagaState state, String errorMessage) {
        state.setStatus(SagaState.STATUS_FAILED);
        state.setErrorMessage(errorMessage);
        state.setUpdatedAt(LocalDateTime.now());
        log.error("Saga [{}] id={} FAILED: {}", state.getSagaType(), state.getId(), errorMessage);
        return repository.save(state);
    }

    public Mono<SagaState> startCompensation(SagaState state) {
        state.setStatus(SagaState.STATUS_COMPENSATING);
        state.setUpdatedAt(LocalDateTime.now());
        log.warn("Saga [{}] id={} starting COMPENSATION from step [{}]",
                state.getSagaType(), state.getId(), state.getCurrentStep());
        return repository.save(state);
    }

    public Mono<SagaState> completeCompensation(SagaState state) {
        state.setStatus(SagaState.STATUS_COMPENSATED);
        state.setUpdatedAt(LocalDateTime.now());
        log.info("Saga [{}] id={} COMPENSATED successfully", state.getSagaType(), state.getId());
        return repository.save(state);
    }

    public Mono<SagaState> setOrderId(SagaState state, UUID orderId) {
        state.setOrderId(orderId);
        state.setUpdatedAt(LocalDateTime.now());
        return repository.save(state);
    }

    public Mono<SagaState> savePayload(SagaState state, String payload) {
        state.setPayload(payload);
        state.setUpdatedAt(LocalDateTime.now());
        return repository.save(state);
    }
}

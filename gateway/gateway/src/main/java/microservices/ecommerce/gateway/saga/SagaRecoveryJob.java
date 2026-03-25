package microservices.ecommerce.gateway.saga;

import microservices.ecommerce.gateway.entity.SagaState;
import microservices.ecommerce.gateway.repository.SagaStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduled job that detects stuck sagas and triggers compensation.
 *
 * A saga is considered "stuck" when it has not been updated for more than
 * {@code app.saga.recovery.stuck-threshold-minutes} minutes and is not in a
 * terminal state (COMPLETED, FAILED, COMPENSATED).
 *
 * Recovery flow per stuck saga:
 * <ol>
 *   <li>If {@code retryCount >= maxRetries}: mark as FAILED definitively (no more attempts).</li>
 *   <li>Otherwise: increment retryCount, set status to COMPENSATING, run compensation via
 *       {@link SagaCompensationRouter}, then mark as COMPENSATED on success or FAILED on error.</li>
 * </ol>
 */
@Component
public class SagaRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(SagaRecoveryJob.class);

    private final SagaStateRepository repository;
    private final SagaExecutionCoordinator sagaCoordinator;
    private final SagaCompensationRouter compensationRouter;
    private final int stuckThresholdMinutes;
    private final int maxRetries;

    public SagaRecoveryJob(SagaStateRepository repository,
                           SagaExecutionCoordinator sagaCoordinator,
                           SagaCompensationRouter compensationRouter,
                           @Value("${app.saga.recovery.stuck-threshold-minutes}") int stuckThresholdMinutes,
                           @Value("${app.saga.recovery.max-retries}") int maxRetries) {
        this.repository = repository;
        this.sagaCoordinator = sagaCoordinator;
        this.compensationRouter = compensationRouter;
        this.stuckThresholdMinutes = stuckThresholdMinutes;
        this.maxRetries = maxRetries;
    }

    @Scheduled(cron = "${app.saga.recovery.cron}")
    public void recoverStuckSagas() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckThresholdMinutes);

        log.info("SagaRecoveryJob: scanning for sagas stuck before {}", threshold);

        repository.findStuckSagas(threshold)
                .flatMap(saga -> {
                    log.warn("SagaRecoveryJob: found stuck saga [{}] id={} step={} status={} retryCount={} lastUpdated={}",
                            saga.getSagaType(), saga.getId(), saga.getCurrentStep(),
                            saga.getStatus(), saga.getRetryCount(), saga.getUpdatedAt());

                    if (saga.getRetryCount() >= maxRetries) {
                        log.error("SagaRecoveryJob: saga id={} exceeded max retries ({}). Marking as FAILED.",
                                saga.getId(), maxRetries);
                        return sagaCoordinator.failSaga(saga,
                                "Exceeded max recovery retries (" + maxRetries + "). Last stuck step: " + saga.getCurrentStep());
                    }

                    // Increment retryCount and start compensation
                    saga.setRetryCount(saga.getRetryCount() + 1);
                    return sagaCoordinator.startCompensation(saga)
                            .flatMap(s -> compensationRouter.compensate(s)
                                    .then(sagaCoordinator.completeCompensation(s))
                                    .onErrorResume(e -> {
                                        log.error("SagaRecoveryJob: compensation failed for saga id={}: {}",
                                                s.getId(), e.getMessage());
                                        return sagaCoordinator.failSaga(s,
                                                "Recovery compensation failed: " + e.getMessage());
                                    }));
                })
                .doOnComplete(() -> log.info("SagaRecoveryJob: scan completed"))
                .subscribe(
                        updated -> log.info("SagaRecoveryJob: saga id={} updated to status={}", updated.getId(), updated.getStatus()),
                        error -> log.error("SagaRecoveryJob: unexpected error during recovery scan", error)
                );
    }
}

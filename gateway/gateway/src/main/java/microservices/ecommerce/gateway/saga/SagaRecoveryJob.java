package microservices.ecommerce.gateway.saga;

import microservices.ecommerce.gateway.entity.SagaState;
import microservices.ecommerce.gateway.repository.SagaStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SagaRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(SagaRecoveryJob.class);

    private final SagaStateRepository repository;
    private final int stuckThresholdMinutes;

    public SagaRecoveryJob(SagaStateRepository repository,
                           @Value("${app.saga.recovery.stuck-threshold-minutes}") int stuckThresholdMinutes) {
        this.repository = repository;
        this.stuckThresholdMinutes = stuckThresholdMinutes;
    }

    @Scheduled(cron = "${app.saga.recovery.cron}")
    public void recoverStuckSagas() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckThresholdMinutes);

        log.info("Saga Recovery Job: scanning for sagas stuck before {}", threshold);

        repository.findStuckSagas(threshold)
                .flatMap(saga -> {
                    log.warn("Saga Recovery: Found stuck saga [{}] id={} step={} status={} lastUpdated={}",
                            saga.getSagaType(), saga.getId(), saga.getCurrentStep(),
                            saga.getStatus(), saga.getUpdatedAt());

                    saga.setStatus(SagaState.STATUS_FAILED);
                    saga.setErrorMessage("Marked as FAILED by recovery job - stuck in step: " + saga.getCurrentStep());
                    saga.setUpdatedAt(LocalDateTime.now());
                    return repository.save(saga);
                })
                .doOnComplete(() -> log.info("Saga Recovery Job: scan completed"))
                .subscribe();
    }
}

package microservices.ecommerce.gateway.repository;

import microservices.ecommerce.gateway.entity.SagaState;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface SagaStateRepository extends R2dbcRepository<SagaState, UUID> {

    @Query("SELECT * FROM saga_states WHERE status NOT IN ('COMPLETED', 'FAILED', 'COMPENSATED') AND updated_at < :threshold")
    Flux<SagaState> findStuckSagas(LocalDateTime threshold);
}

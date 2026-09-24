package microservices.ecommerce.users.infrastructure.adapters.out.repositories;

import microservices.ecommerce.users.infrastructure.adapters.out.entities.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshTokenEntity t
               set t.revokedAt = :revokedAt, t.replacedBy = :replacedBy
             where t.id = :id and t.revokedAt is null
            """)
    int revokeIfActive(@Param("id") UUID id,
                       @Param("revokedAt") Instant revokedAt,
                       @Param("replacedBy") UUID replacedBy);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshTokenEntity t
               set t.revokedAt = :revokedAt
             where t.familyId = :familyId and t.revokedAt is null
            """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);
}

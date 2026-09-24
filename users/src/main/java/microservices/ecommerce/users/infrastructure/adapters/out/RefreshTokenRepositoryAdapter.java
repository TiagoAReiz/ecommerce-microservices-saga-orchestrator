package microservices.ecommerce.users.infrastructure.adapters.out;

import microservices.ecommerce.users.application.ports.out.RefreshTokenRepository;
import microservices.ecommerce.users.core.entities.RefreshToken;
import microservices.ecommerce.users.infrastructure.adapters.out.entities.RefreshTokenEntity;
import microservices.ecommerce.users.infrastructure.adapters.out.repositories.RefreshTokenJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return toDomain(jpaRepository.save(toEntity(token)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(RefreshTokenRepositoryAdapter::toDomain);
    }

    @Override
    public boolean revokeIfActive(UUID tokenId, Instant revokedAt, UUID replacedBy) {
        return jpaRepository.revokeIfActive(tokenId, revokedAt, replacedBy) == 1;
    }

    @Override
    public int revokeFamily(UUID familyId, Instant revokedAt) {
        return jpaRepository.revokeFamily(familyId, revokedAt);
    }

    private static RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(e.getId(), e.getUserId(), e.getFamilyId(), e.getTokenHash(),
                e.getExpiresAt(), e.getCreatedAt(), e.getRevokedAt(), e.getReplacedBy());
    }

    private static RefreshTokenEntity toEntity(RefreshToken t) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.setId(t.getId());
        e.setUserId(t.getUserId());
        e.setFamilyId(t.getFamilyId());
        e.setTokenHash(t.getTokenHash());
        e.setExpiresAt(t.getExpiresAt());
        e.setCreatedAt(t.getCreatedAt());
        e.setRevokedAt(t.getRevokedAt());
        e.setReplacedBy(t.getReplacedBy());
        return e;
    }
}

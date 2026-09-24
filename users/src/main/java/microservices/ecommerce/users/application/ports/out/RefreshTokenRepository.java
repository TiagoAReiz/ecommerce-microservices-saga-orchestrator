package microservices.ecommerce.users.application.ports.out;

import microservices.ecommerce.users.core.entities.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Atomically revokes the token if it is still active (compare-and-set on {@code revoked_at IS NULL}).
     *
     * @return {@code true} only for the single caller that actually revoked it; {@code false} when it had
     *         already been revoked (e.g. a concurrent refresh with the same token won the race)
     */
    boolean revokeIfActive(UUID tokenId, Instant revokedAt, UUID replacedBy);

    /** Revokes every still-active token of the family. @return how many were revoked */
    int revokeFamily(UUID familyId, Instant revokedAt);
}

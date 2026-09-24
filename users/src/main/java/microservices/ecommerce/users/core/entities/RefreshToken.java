package microservices.ecommerce.users.core.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * A rotating refresh token. {@code tokenHash} is the SHA-256 of the opaque value handed to the client;
 * the raw value is never persisted. Every token issued from the same login shares a {@code familyId}.
 */
public class RefreshToken {

    private UUID id;
    private UUID userId;
    private UUID familyId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant revokedAt;
    private UUID replacedBy;

    public RefreshToken() {}

    public RefreshToken(UUID id, UUID userId, UUID familyId, String tokenHash,
                        Instant expiresAt, Instant createdAt, Instant revokedAt, UUID replacedBy) {
        this.id = id;
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
        this.replacedBy = replacedBy;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getFamilyId() { return familyId; }
    public void setFamilyId(UUID familyId) { this.familyId = familyId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public UUID getReplacedBy() { return replacedBy; }
    public void setReplacedBy(UUID replacedBy) { this.replacedBy = replacedBy; }
}

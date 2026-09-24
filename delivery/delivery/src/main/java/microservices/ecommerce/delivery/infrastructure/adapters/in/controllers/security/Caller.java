package microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.security;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Identity of the caller as forwarded by the gateway ({@code X-User-Id} / {@code X-User-Roles}).
 * <p>
 * The gateway strips these headers from client requests and only sets them from a verified JWT, so on
 * requests routed through the gateway they are trustworthy. Requests without {@code X-User-Id} are the
 * saga orchestrator's internal service-to-service calls (the gateway never forwards an authenticated
 * route without it); they are treated as trusted. This relies on the services not being reachable from
 * outside the internal network, see the README.
 */
public record Caller(UUID userId, Set<String> roles) {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String ADMIN_ROLE = "ADMIN";

    public static Caller from(String userIdHeader, String rolesHeader) {
        UUID userId = null;
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                userId = UUID.fromString(userIdHeader.trim());
            } catch (IllegalArgumentException e) {
                throw new AccessDeniedException("Malformed " + USER_ID_HEADER);
            }
        }
        Set<String> roles = rolesHeader == null ? Set.of() : Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return new Caller(userId, roles);
    }

    /** Internal call from the orchestrator (no end-user identity attached). */
    public boolean isInternal() {
        return userId == null;
    }

    public boolean isAdmin() {
        return roles.contains(ADMIN_ROLE);
    }

    /** Owner, ADMIN or internal call. A resource without an owner is only visible to ADMIN/internal. */
    public boolean canAccess(UUID ownerId) {
        return isInternal() || isAdmin() || (ownerId != null && ownerId.equals(userId));
    }

    /** Back-office operations: ADMIN or internal call only (defence in depth behind the gateway rule). */
    public void requireAdminOrInternal() {
        if (!isInternal() && !isAdmin()) {
            throw new AccessDeniedException("ADMIN role required");
        }
    }
}

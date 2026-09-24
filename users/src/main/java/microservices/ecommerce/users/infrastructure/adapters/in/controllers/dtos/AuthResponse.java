package microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos;

import java.util.List;
import java.util.UUID;

/**
 * @param token        JWT access token, sent as {@code Authorization: Bearer <token>}
 * @param expiresIn    access token lifetime in seconds
 * @param refreshToken opaque single-use token for {@code POST /api/v1/auth/refresh}
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        String refreshToken,
        UUID userId,
        String username,
        List<String> roles
) {}

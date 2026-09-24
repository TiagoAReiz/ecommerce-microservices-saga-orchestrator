package microservices.ecommerce.users.application.ports.in.usecases;

import java.util.List;
import java.util.UUID;

/**
 * @param token            short-lived JWT access token
 * @param expiresInSeconds lifetime of {@code token}
 * @param refreshToken     opaque, single-use refresh token (rotated by {@link AuthUseCase#refresh(String)})
 */
public record AuthResult(String token, long expiresInSeconds, String refreshToken,
                         UUID userId, String username, List<String> roles) {}

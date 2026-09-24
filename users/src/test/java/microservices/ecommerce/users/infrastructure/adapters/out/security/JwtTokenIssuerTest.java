package microservices.ecommerce.users.infrastructure.adapters.out.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import microservices.ecommerce.users.core.entities.User;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test: the token must be readable exactly the way the gateway's JwtUtil reads it
 * (same HS256 key derivation, {@code sub} = user UUID, {@code roles} = JSON array of strings),
 * because the gateway forwards those as X-User-Id / X-User-Roles.
 */
class JwtTokenIssuerTest {

    private static final String SECRET = "test-only-secret-key-with-at-least-32-bytes-for-hs256";

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(SECRET, 60_000);

    @Test
    void issue_claimsMatchWhatTheGatewayExpects() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice", "alice@example.com", "hashed", List.of("USER"), LocalDateTime.now());

        String token = issuer.issue(user);

        // Parsed the same way as gateway JwtUtil.parseClaims
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(UUID.fromString(claims.getSubject())).isEqualTo(userId);
        // gateway JwtUtil.getRoles only accepts a List
        assertThat(claims.get("roles")).isInstanceOf(List.class);
        assertThat(claims.get("roles", List.class)).containsExactly("USER");
        assertThat(claims.getExpiration()).isAfter(new Date());
        assertThat(claims.getIssuedAt()).isNotNull();
    }

    @Test
    void issue_usesHs256() {
        User user = new User(UUID.randomUUID(), "alice", "a@example.com", "hashed", List.of("USER"), LocalDateTime.now());

        String token = issuer.issue(user);

        String header = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
        assertThat(header).contains("\"alg\":\"HS256\"");
    }

    @Test
    void constructor_rejectsSecretsShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtTokenIssuer("too-short", 60_000))
                .isInstanceOf(WeakKeyException.class);
    }
}

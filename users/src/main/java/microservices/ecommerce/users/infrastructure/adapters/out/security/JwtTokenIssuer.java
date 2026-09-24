package microservices.ecommerce.users.infrastructure.adapters.out.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import microservices.ecommerce.users.application.ports.out.TokenIssuer;
import microservices.ecommerce.users.core.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues HS256 JWTs in the shape the gateway's JwtAuthenticationFilter expects:
 * {@code sub} = user UUID (forwarded as {@code X-User-Id}) and {@code roles} = list of strings
 * (forwarded as {@code X-User-Roles}). The key is derived the same way as the gateway's JwtUtil:
 * the UTF-8 bytes of {@code app.jwt.secret}.
 */
@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenIssuer(@Value("${app.jwt.secret}") String secret,
                          @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // Throws WeakKeyException for secrets shorter than 32 bytes, so a bad secret fails at startup.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String issue(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("roles", user.getRoles())
                .claim("username", user.getUsername())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}

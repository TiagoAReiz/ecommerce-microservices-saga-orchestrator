package microservices.ecommerce.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import microservices.ecommerce.gateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "my-super-secret-jwt-key-that-should-be-at-least-256-bits-long-for-hs256";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtAuthenticationFilter filter;
    private WebFilterChain chain;

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);
        List<String> publicPaths = List.of("/api/v1/auth/**", "/api/v1/products/**", "/actuator/**");
        List<String> userScopedPaths = List.of("/api/v1/carts/{userId}/**", "/api/v1/orders/user/{userId}");
        filter = new JwtAuthenticationFilter(jwtUtil, publicPaths, userScopedPaths);

        chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_publicPath_bypassesAuthentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/products/123").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_actuatorPath_bypassesAuthentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_authPath_isPublic() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        verify(chain).filter(any());
    }

    @Test
    void filter_validBearerToken_injectsUserHeaders() {
        String userId = UUID.randomUUID().toString();
        String token = buildToken(userId, List.of("USER", "ADMIN"), 60_000);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        HttpHeaders forwarded = forwardedHeaders();
        assertThat(forwarded.get(JwtAuthenticationFilter.USER_ID_HEADER)).containsExactly(userId);
        assertThat(forwarded.get(JwtAuthenticationFilter.USER_ROLES_HEADER)).containsExactly("USER,ADMIN");
    }

    @Test
    void filter_spoofedUserHeaders_areReplacedByTokenIdentity() {
        String userId = UUID.randomUUID().toString();
        String token = buildToken(userId, List.of("USER"), 60_000);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/checkout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(JwtAuthenticationFilter.USER_ID_HEADER, UUID.randomUUID().toString())
                        .header(JwtAuthenticationFilter.USER_ROLES_HEADER, "ADMIN")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        HttpHeaders forwarded = forwardedHeaders();
        assertThat(forwarded.get(JwtAuthenticationFilter.USER_ID_HEADER)).containsExactly(userId);
        assertThat(forwarded.get(JwtAuthenticationFilter.USER_ROLES_HEADER)).containsExactly("USER");
    }

    @Test
    void filter_spoofedUserHeadersOnPublicPath_areStripped() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/products/123")
                        .header(JwtAuthenticationFilter.USER_ID_HEADER, UUID.randomUUID().toString())
                        .header(JwtAuthenticationFilter.USER_ROLES_HEADER, "ADMIN")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        HttpHeaders forwarded = forwardedHeaders();
        assertThat(forwarded.containsHeader(JwtAuthenticationFilter.USER_ID_HEADER)).isFalse();
        assertThat(forwarded.containsHeader(JwtAuthenticationFilter.USER_ROLES_HEADER)).isFalse();
    }

    @Test
    void filter_userScopedPathOfAnotherUser_returns403() {
        String token = buildToken(UUID.randomUUID().toString(), List.of("USER"), 60_000);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/carts/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_nestedUserScopedPathOfAnotherUser_returns403() {
        String token = buildToken(UUID.randomUUID().toString(), List.of("USER"), 60_000);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/carts/" + UUID.randomUUID() + "/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void filter_userScopedPathOfSameUser_passes() {
        String userId = UUID.randomUUID().toString();
        String token = buildToken(userId, List.of("USER"), 60_000);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/carts/" + userId + "/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        verify(chain).filter(any());
    }

    @Test
    void filter_missingAuthorizationHeader_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_missingBearerPrefix_returns401() {
        String token = buildToken(UUID.randomUUID().toString(), List.of("ROLE_USER"), 60_000);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, token)  // missing "Bearer " prefix
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_expiredToken_returns401() {
        String token = buildToken(UUID.randomUUID().toString(), List.of("ROLE_USER"), -1000); // already expired

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_invalidToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not.a.valid.jwt")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getOrder_isMinusOne() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }

    private HttpHeaders forwardedHeaders() {
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        return captor.getValue().getRequest().getHeaders();
    }

    private String buildToken(String subject, List<String> roles, long expiryMs) {
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(KEY)
                .compact();
    }
}

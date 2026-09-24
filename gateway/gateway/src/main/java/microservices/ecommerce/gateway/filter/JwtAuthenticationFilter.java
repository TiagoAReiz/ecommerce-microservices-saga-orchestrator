package microservices.ecommerce.gateway.filter;

import io.jsonwebtoken.Claims;
import microservices.ecommerce.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Validates the Bearer JWT for every request that reaches the gateway.
 * <p>
 * Implemented as a WebFlux {@link WebFilter} (not a gateway GlobalFilter) so it protects both the
 * proxied routes and the orchestration endpoints served by the gateway itself
 * ({@code /api/v1/checkout}, {@code /api/v1/orders/{id}/cancel}).
 * <p>
 * Identity headers ({@code X-User-Id}, {@code X-User-Roles}) are only ever set from a verified token:
 * any client-supplied values are stripped first, on public paths too. Paths matching
 * {@code app.jwt.user-scoped-paths} carry a {@code {userId}} segment that must equal the token subject,
 * so one user cannot read or modify another user's resources (e.g. carts) by changing the URL.
 */
@Component
public class JwtAuthenticationFilter implements WebFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_VARIABLE = "userId";

    private final JwtUtil jwtUtil;
    private final List<String> publicPaths;
    private final List<String> userScopedPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   @Value("${app.jwt.public-paths}") List<String> publicPaths,
                                   @Value("${app.jwt.user-scoped-paths:}") List<String> userScopedPaths) {
        this.jwtUtil = jwtUtil;
        this.publicPaths = publicPaths;
        this.userScopedPaths = userScopedPaths;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Never trust identity headers coming from the client
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLES_HEADER);
                })
                .build();
        String path = request.getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange.mutate().request(request).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!jwtUtil.isTokenValid(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        Claims claims = jwtUtil.parseClaims(token);
        String userId = claims.getSubject();

        if (!isAllowedForUser(path, userId)) {
            log.warn("User {} denied access to another user's resource: {}", userId, path);
            return reject(exchange, HttpStatus.FORBIDDEN);
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(USER_ID_HEADER, userId)
                .header(USER_ROLES_HEADER, String.join(",", jwtUtil.getRoles(token)))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isAllowedForUser(String path, String userId) {
        for (String pattern : userScopedPaths) {
            if (pathMatcher.match(pattern, path)) {
                Map<String, String> variables = pathMatcher.extractUriTemplateVariables(pattern, path);
                String pathUserId = variables.get(USER_ID_VARIABLE);
                if (pathUserId != null && !pathUserId.equalsIgnoreCase(userId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

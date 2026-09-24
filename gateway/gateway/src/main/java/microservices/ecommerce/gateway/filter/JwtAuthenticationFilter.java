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
import java.util.Set;

/**
 * Authentication and coarse-grained authorization for every request that reaches the gateway.
 * <p>
 * Implemented as a WebFlux {@link WebFilter} (not a gateway GlobalFilter) so it protects both the
 * proxied routes and the orchestration endpoints served by the gateway itself
 * ({@code /api/v1/checkout}, {@code /api/v1/orders/{id}/cancel}).
 * <ol>
 *   <li>Client-supplied {@code X-User-Id} / {@code X-User-Roles} are always stripped, on public paths too.</li>
 *   <li>Paths that are not normalized (dot segments, {@code //}, {@code ;} path parameters, backslashes)
 *       are rejected with 400, so the rules below cannot be sidestepped by path tricks.</li>
 *   <li>{@code app.jwt.public-paths} skip authentication.</li>
 *   <li>Everything else needs a valid Bearer JWT (401 otherwise).</li>
 *   <li>{@code app.security.admin-paths} additionally require the {@code ADMIN} role (403 otherwise).</li>
 *   <li>{@code app.jwt.user-scoped-paths} carry a {@code {userId}} segment that must equal the token subject
 *       (403 otherwise); an ADMIN may read (GET/HEAD) other users' resources.</li>
 *   <li>The verified identity is forwarded as {@code X-User-Id} / {@code X-User-Roles}. Per-resource
 *       ownership (orders, payments, deliveries by id) is enforced by the owning services from these headers.</li>
 * </ol>
 * Rules use the {@link RouteRule} syntax {@code "[METHOD|METHOD ]/ant/pattern/**"}.
 */
@Component
public class JwtAuthenticationFilter implements WebFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String ADMIN_ROLE = "ADMIN";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_VARIABLE = "userId";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD");

    private final JwtUtil jwtUtil;
    private final List<RouteRule> publicRules;
    private final List<RouteRule> adminRules;
    private final List<String> userScopedPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   @Value("${app.jwt.public-paths}") List<String> publicPaths,
                                   @Value("${app.security.admin-paths:}") List<String> adminPaths,
                                   @Value("${app.jwt.user-scoped-paths:}") List<String> userScopedPaths) {
        this.jwtUtil = jwtUtil;
        this.publicRules = parse(publicPaths);
        this.adminRules = parse(adminPaths);
        this.userScopedPaths = userScopedPaths.stream().filter(p -> !p.isBlank()).toList();
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
        String method = request.getMethod().name();

        if (!isNormalized(request.getURI().getRawPath()) || !isNormalized(path)) {
            log.warn("Rejecting non-normalized path: {}", request.getURI().getRawPath());
            return reject(exchange, HttpStatus.BAD_REQUEST);
        }

        if (matchesAny(publicRules, method, path)) {
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
        List<String> roles = jwtUtil.getRoles(claims);
        boolean admin = roles.contains(ADMIN_ROLE);

        if (!admin && matchesAny(adminRules, method, path)) {
            log.warn("User {} without ADMIN denied {} {}", userId, method, path);
            return reject(exchange, HttpStatus.FORBIDDEN);
        }

        if (!isAllowedForUser(path, userId) && !(admin && SAFE_METHODS.contains(method))) {
            log.warn("User {} denied access to another user's resource: {}", userId, path);
            return reject(exchange, HttpStatus.FORBIDDEN);
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(USER_ID_HEADER, userId)
                .header(USER_ROLES_HEADER, String.join(",", roles))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean matchesAny(List<RouteRule> rules, String method, String path) {
        return rules.stream().anyMatch(rule -> rule.matches(method, path, pathMatcher));
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

    /**
     * Downstream servers normalize paths ({@code /a/../b} becomes {@code /b}, {@code ;params} are dropped),
     * so a path that is not already normalized could match different rules here than the handler it
     * finally reaches. Such paths have no legitimate use in this API and are rejected.
     */
    static boolean isNormalized(String path) {
        if (path == null || path.isEmpty()) {
            return true;
        }
        if (path.contains("//") || path.contains(";") || path.contains("\\")) {
            return false;
        }
        for (String segment : path.split("/")) {
            if (segment.equals(".") || segment.equals("..")) {
                return false;
            }
        }
        return true;
    }

    private static List<RouteRule> parse(List<String> specs) {
        return specs.stream().filter(s -> !s.isBlank()).map(RouteRule::parse).toList();
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

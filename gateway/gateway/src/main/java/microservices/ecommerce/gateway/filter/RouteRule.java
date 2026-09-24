package microservices.ecommerce.gateway.filter;

import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A path rule with an optional HTTP method restriction, written as {@code "[METHODS ]pattern"}:
 * <ul>
 *   <li>{@code /api/v1/auth/**} - any method</li>
 *   <li>{@code GET /api/v1/products/**} - only GET</li>
 *   <li>{@code POST|PUT|PATCH|DELETE /api/v1/products/**} - any of the listed methods</li>
 * </ul>
 * Patterns are Ant-style ({@link AntPathMatcher}).
 */
public record RouteRule(Set<String> methods, String pattern) {

    public static RouteRule parse(String spec) {
        String trimmed = spec.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new RouteRule(Set.of(), trimmed);
        }
        Set<String> methods = Arrays.stream(trimmed.substring(0, space).split("\\|"))
                .map(m -> m.trim().toUpperCase(Locale.ROOT))
                .filter(m -> !m.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return new RouteRule(methods, trimmed.substring(space + 1).trim());
    }

    public boolean matches(String method, String path, AntPathMatcher matcher) {
        return (methods.isEmpty() || methods.contains(method)) && matcher.match(pattern, path);
    }
}

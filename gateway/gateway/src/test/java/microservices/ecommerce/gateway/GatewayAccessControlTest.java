package microservices.ecommerce.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import microservices.ecommerce.gateway.filter.JwtAuthenticationFilter;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs requests through the real gateway (application.yml routes and access rules, security filter chain)
 * with every downstream service replaced by a MockWebServer, to prove the configured rules end to end:
 * unauthenticated -> 401, USER on an admin route -> 403 (never forwarded), ADMIN -> forwarded with its
 * verified identity. Needs PostgreSQL like {@link GatewayApplicationTests}.
 */
@SpringBootTest(properties = "app.jwt.secret=" + GatewayAccessControlTest.SECRET)
class GatewayAccessControlTest {

    static final String SECRET = "test-only-secret-key-with-at-least-32-bytes-for-hs256";

    private static final MockWebServer downstream = startDownstream();

    @Autowired
    private ApplicationContext context;

    private WebTestClient client;

    @DynamicPropertySource
    static void downstreamUrls(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + downstream.getPort();
        for (String service : List.of("PRODUCTS", "INVENTORY", "CART", "ORDER", "PAYMENT", "DELIVERY", "USERS")) {
            registry.add(service + "_SERVICE_URL", () -> url);
        }
    }

    @BeforeEach
    void setUp() throws InterruptedException {
        client = WebTestClient.bindToApplicationContext(context).configureClient()
                .responseTimeout(java.time.Duration.ofSeconds(15)).build();
        // drain requests recorded by previous tests
        while (downstream.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
            // discard
        }
    }

    @AfterAll
    static void shutdown() throws IOException {
        downstream.shutdown();
    }

    @Test
    void productCatalogue_isPublicForReading() throws Exception {
        client.get().uri("/api/v1/products").exchange().expectStatus().isOk();

        RecordedRequest forwarded = downstream.takeRequest(1, TimeUnit.SECONDS);
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getHeader(JwtAuthenticationFilter.USER_ID_HEADER)).isNull();
    }

    @Test
    void adminRoute_withoutToken_is401() {
        client.post().uri("/api/v1/products").bodyValue("{}")
                .exchange().expectStatus().isUnauthorized();

        assertNothingForwarded();
    }

    @Test
    void adminRoutes_withUserRole_are403AndNeverForwarded() {
        String userToken = token(UUID.randomUUID(), List.of("USER"));

        expect(HttpMethod.POST, "/api/v1/products", userToken, HttpStatus.FORBIDDEN);
        expect(HttpMethod.DELETE, "/api/v1/products/" + UUID.randomUUID(), userToken, HttpStatus.FORBIDDEN);
        expect(HttpMethod.POST, "/api/v1/inventory/stock", userToken, HttpStatus.FORBIDDEN);
        expect(HttpMethod.POST, "/api/v1/orders", userToken, HttpStatus.FORBIDDEN);
        expect(HttpMethod.PATCH, "/api/v1/orders/" + UUID.randomUUID() + "/status?status=SHIPPED", userToken,
                HttpStatus.FORBIDDEN);
        expect(HttpMethod.POST, "/api/v1/payments", userToken, HttpStatus.FORBIDDEN);
        expect(HttpMethod.PATCH, "/api/v1/deliveries/" + UUID.randomUUID() + "/status?status=SHIPPED", userToken,
                HttpStatus.FORBIDDEN);

        assertNothingForwarded();
    }

    @Test
    void adminRoutes_withAdminRole_areForwardedWithVerifiedIdentity() throws Exception {
        UUID adminId = UUID.randomUUID();
        String adminToken = token(adminId, List.of("USER", "ADMIN"));

        expect(HttpMethod.POST, "/api/v1/products", adminToken, HttpStatus.OK);

        RecordedRequest forwarded = downstream.takeRequest(1, TimeUnit.SECONDS);
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getPath()).isEqualTo("/api/v1/products");
        assertThat(forwarded.getHeader(JwtAuthenticationFilter.USER_ID_HEADER)).isEqualTo(adminId.toString());
        assertThat(forwarded.getHeader(JwtAuthenticationFilter.USER_ROLES_HEADER)).isEqualTo("USER,ADMIN");

        expect(HttpMethod.PATCH, "/api/v1/orders/" + UUID.randomUUID() + "/status?status=SHIPPED", adminToken,
                HttpStatus.OK);
        assertThat(downstream.takeRequest(1, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void resourceReads_areForwardedWithTheCallersIdentityForOwnershipChecks() throws Exception {
        UUID userId = UUID.randomUUID();
        String userToken = token(userId, List.of("USER"));

        expect(HttpMethod.GET, "/api/v1/orders/" + UUID.randomUUID(), userToken, HttpStatus.OK);

        RecordedRequest forwarded = downstream.takeRequest(1, TimeUnit.SECONDS);
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getHeader(JwtAuthenticationFilter.USER_ID_HEADER)).isEqualTo(userId.toString());
        assertThat(forwarded.getHeader(JwtAuthenticationFilter.USER_ROLES_HEADER)).isEqualTo("USER");
    }

    private void expect(HttpMethod method, String uri, String token, HttpStatus status) {
        WebTestClient.RequestBodySpec request = client.method(method).uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        WebTestClient.RequestHeadersSpec<?> ready = method == HttpMethod.GET
                ? request
                : request.header("Content-Type", "application/json").bodyValue("{}");
        ready.exchange().expectStatus().isEqualTo(status);
    }

    private static void assertNothingForwarded() {
        try {
            assertThat(downstream.takeRequest(200, TimeUnit.MILLISECONDS)).isNull();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static String token(UUID userId, List<String> roles) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static MockWebServer startDownstream() {
        MockWebServer server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody("{}");
            }
        });
        try {
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return server;
    }
}

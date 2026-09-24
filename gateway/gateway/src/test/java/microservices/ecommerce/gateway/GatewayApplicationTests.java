package microservices.ecommerce.gateway;

import microservices.ecommerce.gateway.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Full context test: needs a reachable PostgreSQL (see SPRING_R2DBC_URL / SPRING_FLYWAY_URL).
@SpringBootTest(properties = "app.jwt.secret=test-only-secret-key-with-at-least-32-bytes-for-hs256")
class GatewayApplicationTests {

	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Value("${app.jwt.public-paths}")
	private List<String> publicPaths;

	@Value("${app.jwt.user-scoped-paths}")
	private List<String> userScopedPaths;

	@Test
	void contextLoads() {
		assertThat(jwtAuthenticationFilter).isNotNull();
	}

	@Test
	void registrationAndLoginArePublic_cartsAreUserScoped() {
		assertThat(publicPaths).contains("/api/v1/auth/**");
		assertThat(userScopedPaths).contains("/api/v1/carts/{userId}/**", "/api/v1/orders/user/{userId}");
	}

}

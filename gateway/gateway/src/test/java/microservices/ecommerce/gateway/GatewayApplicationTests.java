package microservices.ecommerce.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Full context test: needs a reachable PostgreSQL (see SPRING_R2DBC_URL / SPRING_FLYWAY_URL).
@SpringBootTest(properties = "app.jwt.secret=test-only-secret-key-with-at-least-32-bytes-for-hs256")
class GatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}

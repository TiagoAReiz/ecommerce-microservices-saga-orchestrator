package microservices.ecommerce.users;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Full context test: needs a reachable PostgreSQL (SPRING_DATASOURCE_*); Flyway migrates and Hibernate validates.
@SpringBootTest(properties = "app.jwt.secret=test-only-secret-key-with-at-least-32-bytes-for-hs256")
class UsersApplicationTests {

	@Test
	void contextLoads() {
	}

}

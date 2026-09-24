package microservices.ecommerce.users;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end over the real PostgreSQL schema (Flyway V2): rotation, reuse detection committing the family
 * revocation even though the request fails, logout, and ADMIN_EMAILS bootstrap.
 */
@SpringBootTest(properties = {
        "app.jwt.secret=test-only-secret-key-with-at-least-32-bytes-for-hs256",
        "app.security.admin-emails=root-admin@example.com"
})
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void register_refreshRotates_reuseRevokesFamily_logoutRevokes() throws Exception {
        String username = "it-" + UUID.randomUUID().toString().substring(0, 8);
        String registered = body(post("/api/v1/auth/register", """
                {"username":"%s","email":"%s@example.com","password":"s3cret-pass"}
                """.formatted(username, username)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.contains("USER"))));
        String r1 = JsonPath.read(registered, "$.refreshToken");

        // the raw token is never stored
        Integer rawStored = jdbcTemplate.queryForObject(
                "select count(*) from refresh_tokens where token_hash = ?", Integer.class, r1);
        assertThat(rawStored).isZero();

        // rotation
        String r2 = JsonPath.read(body(refresh(r1).andExpect(status().isOk())), "$.refreshToken");
        assertThat(r2).isNotEqualTo(r1);

        // reuse of r1 -> 401 and r2 is revoked as well (revocation committed despite the error)
        refresh(r1).andExpect(status().isUnauthorized());
        refresh(r2).andExpect(status().isUnauthorized());

        // a fresh login is a new family; logout revokes it
        String loggedIn = body(post("/api/v1/auth/login", """
                {"username":"%s","password":"s3cret-pass"}
                """.formatted(username)).andExpect(status().isOk()));
        String r3 = JsonPath.read(loggedIn, "$.refreshToken");
        post("/api/v1/auth/logout", "{\"refreshToken\":\"" + r3 + "\"}").andExpect(status().isNoContent());
        refresh(r3).andExpect(status().isUnauthorized());

        // logout with an unknown token does not reveal anything
        post("/api/v1/auth/logout", "{\"refreshToken\":\"unknown\"}").andExpect(status().isNoContent());
    }

    @Test
    void adminEmails_grantAdminOnRegistration_andItIsPersisted() throws Exception {
        jdbcTemplate.update("delete from users where email = 'root-admin@example.com'");

        String response = body(post("/api/v1/auth/register", """
                {"username":"root-admin-%s","email":"root-admin@example.com","password":"s3cret-pass"}
                """.formatted(UUID.randomUUID().toString().substring(0, 8))).andExpect(status().isCreated()));

        List<String> roles = JsonPath.read(response, "$.roles");
        assertThat(roles).containsExactly("USER", "ADMIN");
        String stored = jdbcTemplate.queryForObject(
                "select roles from users where email = 'root-admin@example.com'", String.class);
        assertThat(stored).isEqualTo("USER,ADMIN");
    }

    private ResultActions refresh(String token) throws Exception {
        return post("/api/v1/auth/refresh", "{\"refreshToken\":\"" + token + "\"}");
    }

    private ResultActions post(String url, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }
}

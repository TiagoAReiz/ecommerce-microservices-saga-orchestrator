package microservices.ecommerce.users.infrastructure.adapters.in.controllers;

import microservices.ecommerce.users.application.ports.in.usecases.AuthResult;
import microservices.ecommerce.users.application.ports.in.usecases.AuthUseCase;
import microservices.ecommerce.users.application.ports.in.usecases.LoginCommand;
import microservices.ecommerce.users.application.ports.in.usecases.RegisterCommand;
import microservices.ecommerce.users.core.exceptions.InvalidCredentialsException;
import microservices.ecommerce.users.core.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String REGISTER_BODY = """
            {"username":"alice","email":"alice@example.com","password":"s3cret-pass"}
            """;
    private static final String LOGIN_BODY = """
            {"username":"alice","password":"s3cret-pass"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthUseCase authUseCase;

    @Test
    void register_validRequest_returns201WithToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authUseCase.register(new RegisterCommand("alice", "alice@example.com", "s3cret-pass")))
                .thenReturn(new AuthResult("jwt-token", userId, "alice", List.of("USER")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void register_duplicateUser_returns409() throws Exception {
        when(authUseCase.register(any())).thenThrow(new UserAlreadyExistsException("Username already taken"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken"));
    }

    @Test
    void register_invalidBody_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","email":"not-an-email","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.username").exists())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());

        verify(authUseCase, never()).register(any());
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authUseCase.login(new LoginCommand("alice", "s3cret-pass")))
                .thenReturn(new AuthResult("jwt-token", userId, "alice", List.of("USER")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authUseCase.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.username").exists())
                .andExpect(jsonPath("$.fields.password").exists());

        verify(authUseCase, never()).login(any());
    }
}

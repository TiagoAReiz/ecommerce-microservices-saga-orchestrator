package microservices.ecommerce.users.infrastructure.config;

import microservices.ecommerce.users.application.ports.in.usecases.AuthUseCase;
import microservices.ecommerce.users.application.ports.out.OpaqueTokenGenerator;
import microservices.ecommerce.users.application.ports.out.PasswordHasher;
import microservices.ecommerce.users.application.ports.out.RefreshTokenRepository;
import microservices.ecommerce.users.application.ports.out.TokenIssuer;
import microservices.ecommerce.users.application.ports.out.UserRepository;
import microservices.ecommerce.users.application.services.AdminBootstrapPolicy;
import microservices.ecommerce.users.application.services.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/** Wires the application services with their configuration, keeping the application layer free of @Value. */
@Configuration
public class AuthConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** {@code ADMIN_EMAILS}: comma-separated e-mails granted ADMIN. Empty by default (no admins). */
    @Bean
    public AdminBootstrapPolicy adminBootstrapPolicy(@Value("${app.security.admin-emails:}") List<String> adminEmails) {
        return new AdminBootstrapPolicy(adminEmails);
    }

    @Bean
    public AuthUseCase authUseCase(UserRepository userRepository,
                                   PasswordHasher passwordHasher,
                                   TokenIssuer tokenIssuer,
                                   RefreshTokenRepository refreshTokenRepository,
                                   OpaqueTokenGenerator tokenGenerator,
                                   AdminBootstrapPolicy adminBootstrapPolicy,
                                   Clock clock,
                                   @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        return new AuthService(userRepository, passwordHasher, tokenIssuer, refreshTokenRepository,
                tokenGenerator, adminBootstrapPolicy, clock, Duration.ofMillis(refreshExpirationMs));
    }
}

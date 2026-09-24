package microservices.ecommerce.users.application.services;

import microservices.ecommerce.users.application.ports.in.usecases.AuthResult;
import microservices.ecommerce.users.application.ports.in.usecases.AuthUseCase;
import microservices.ecommerce.users.application.ports.in.usecases.LoginCommand;
import microservices.ecommerce.users.application.ports.in.usecases.RegisterCommand;
import microservices.ecommerce.users.application.ports.out.OpaqueTokenGenerator;
import microservices.ecommerce.users.application.ports.out.PasswordHasher;
import microservices.ecommerce.users.application.ports.out.RefreshTokenRepository;
import microservices.ecommerce.users.application.ports.out.TokenIssuer;
import microservices.ecommerce.users.application.ports.out.UserRepository;
import microservices.ecommerce.users.core.entities.RefreshToken;
import microservices.ecommerce.users.core.entities.User;
import microservices.ecommerce.users.core.exceptions.InvalidCredentialsException;
import microservices.ecommerce.users.core.exceptions.InvalidRefreshTokenException;
import microservices.ecommerce.users.core.exceptions.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registration, login and the refresh-token lifecycle.
 * <p>
 * Sessions: every login/registration starts a new refresh-token <em>family</em>. A refresh token is
 * single-use: {@link #refresh(String)} revokes it and issues a successor in the same family. If a revoked
 * token is presented again, someone replayed it (the legitimate client or an attacker already used it),
 * so the whole family is revoked and both parties have to log in again.
 */
public class AuthService implements AuthUseCase {

    static final String DEFAULT_ROLE = "USER";
    static final String ADMIN_ROLE = "ADMIN";

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OpaqueTokenGenerator tokenGenerator;
    private final AdminBootstrapPolicy adminBootstrapPolicy;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public AuthService(UserRepository userRepository,
                       PasswordHasher passwordHasher,
                       TokenIssuer tokenIssuer,
                       RefreshTokenRepository refreshTokenRepository,
                       OpaqueTokenGenerator tokenGenerator,
                       AdminBootstrapPolicy adminBootstrapPolicy,
                       Clock clock,
                       Duration refreshTokenTtl) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.adminBootstrapPolicy = adminBootstrapPolicy;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Override
    @Transactional
    public AuthResult register(RegisterCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException("Username already taken");
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        List<String> roles = new ArrayList<>(List.of(DEFAULT_ROLE));
        if (adminBootstrapPolicy.isBootstrapAdmin(command.email())) {
            roles.add(ADMIN_ROLE);
            log.info("Granting ADMIN to new account {} (listed in ADMIN_EMAILS)", command.username());
        }

        User user = new User(
                UUID.randomUUID(),
                command.username(),
                command.email(),
                passwordHasher.hash(command.rawPassword()),
                List.copyOf(roles),
                LocalDateTime.now(clock));

        return startSession(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthResult login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .filter(u -> passwordHasher.matches(command.rawPassword(), u.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);

        return startSession(applyAdminBootstrap(user));
    }

    /*
     * noRollbackFor: on reuse detection the family revocation must be committed even though the call
     * then fails with InvalidRefreshTokenException.
     */
    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository.findByTokenHash(tokenGenerator.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            revokeFamilyOnReuse(current, now);
        }
        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);

        UUID successorId = UUID.randomUUID();
        // Compare-and-set: of two concurrent refreshes with the same token only one can win; the loser is
        // treated exactly like a replay.
        if (!refreshTokenRepository.revokeIfActive(current.getId(), now, successorId)) {
            revokeFamilyOnReuse(current, now);
        }

        String rawSuccessor = tokenGenerator.generate();
        refreshTokenRepository.save(new RefreshToken(successorId, user.getId(), current.getFamilyId(),
                tokenGenerator.hash(rawSuccessor), now.plus(refreshTokenTtl), now, null, null));

        return authenticated(applyAdminBootstrap(user), rawSuccessor);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(tokenGenerator.hash(rawRefreshToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    private void revokeFamilyOnReuse(RefreshToken token, Instant now) {
        int revoked = refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
        log.warn("Refresh token reuse detected for user {} (family {}); revoked {} active token(s)",
                token.getUserId(), token.getFamilyId(), revoked);
        throw new InvalidRefreshTokenException();
    }

    /** Adds ADMIN (and persists it) when the account's e-mail is listed in ADMIN_EMAILS. Never removes it. */
    private User applyAdminBootstrap(User user) {
        List<String> roles = user.getRoles() == null ? List.of() : user.getRoles();
        if (roles.contains(ADMIN_ROLE) || !adminBootstrapPolicy.isBootstrapAdmin(user.getEmail())) {
            return user;
        }
        List<String> promoted = new ArrayList<>(roles);
        if (!promoted.contains(DEFAULT_ROLE)) {
            promoted.add(0, DEFAULT_ROLE);
        }
        promoted.add(ADMIN_ROLE);
        user.setRoles(List.copyOf(promoted));
        log.info("Granting ADMIN to existing account {} (listed in ADMIN_EMAILS)", user.getUsername());
        return userRepository.save(user);
    }

    private AuthResult startSession(User user) {
        Instant now = clock.instant();
        String rawRefreshToken = tokenGenerator.generate();
        refreshTokenRepository.save(new RefreshToken(UUID.randomUUID(), user.getId(), UUID.randomUUID(),
                tokenGenerator.hash(rawRefreshToken), now.plus(refreshTokenTtl), now, null, null));
        return authenticated(user, rawRefreshToken);
    }

    private AuthResult authenticated(User user, String rawRefreshToken) {
        return new AuthResult(tokenIssuer.issue(user), tokenIssuer.accessTokenTtlSeconds(), rawRefreshToken,
                user.getId(), user.getUsername(), user.getRoles());
    }
}

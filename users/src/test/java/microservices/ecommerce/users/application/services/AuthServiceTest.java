package microservices.ecommerce.users.application.services;

import microservices.ecommerce.users.application.ports.in.usecases.AuthResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    private InMemoryRefreshTokens refreshTokens;
    private MutableClock clock;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        refreshTokens = new InMemoryRefreshTokens();
        clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        authService = newService(new AdminBootstrapPolicy(List.of("Boss@Example.com")));
        when(tokenIssuer.issue(any())).thenReturn("jwt-token");
        when(tokenIssuer.accessTokenTtlSeconds()).thenReturn(900L);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private AuthService newService(AdminBootstrapPolicy policy) {
        return new AuthService(userRepository, passwordHasher, tokenIssuer, refreshTokens,
                new CountingTokenGenerator(), policy, clock, REFRESH_TTL);
    }

    // --- register / login ---

    @Test
    void register_newUser_hashesPasswordSavesAndReturnsTokens() {
        RegisterCommand command = new RegisterCommand("alice", "alice@example.com", "s3cret-pass");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordHasher.hash("s3cret-pass")).thenReturn("hashed");

        AuthResult result = authService.register(command);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPassword()).isEqualTo("hashed");
        assertThat(user.getRoles()).containsExactly("USER");
        assertThat(user.getCreatedAt()).isNotNull();

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.expiresInSeconds()).isEqualTo(900);
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.roles()).containsExactly("USER");

        // Only the hash is stored
        RefreshToken stored = refreshTokens.single();
        assertThat(stored.getTokenHash()).isEqualTo("hash:" + result.refreshToken());
        assertThat(stored.getUserId()).isEqualTo(user.getId());
        assertThat(stored.getExpiresAt()).isEqualTo(clock.instant().plus(REFRESH_TTL));
    }

    @Test
    void register_emailInAdminEmails_grantsAdmin() {
        when(passwordHasher.hash(any())).thenReturn("hashed");

        AuthResult result = authService.register(new RegisterCommand("boss", "boss@example.com", "s3cret-pass"));

        assertThat(result.roles()).containsExactly("USER", "ADMIN");
    }

    @Test
    void register_withoutAdminEmails_nobodyIsAdmin() {
        authService = newService(new AdminBootstrapPolicy(List.of()));
        when(passwordHasher.hash(any())).thenReturn("hashed");

        AuthResult result = authService.register(new RegisterCommand("boss", "boss@example.com", "s3cret-pass"));

        assertThat(result.roles()).containsExactly("USER");
    }

    @Test
    void register_usernameTaken_throwsConflictAndDoesNotSave() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterCommand("alice", "a@example.com", "s3cret-pass")))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void register_emailTaken_throwsConflictAndDoesNotSave() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("a@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterCommand("alice", "a@example.com", "s3cret-pass")))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsTokens() {
        User user = existingUser("alice", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("s3cret-pass", "hashed")).thenReturn(true);

        AuthResult result = authService.login(new LoginCommand("alice", "s3cret-pass"));

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.roles()).containsExactly("USER");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_existingAccountListedInAdminEmails_isPromotedAndPersisted() {
        User user = existingUser("boss", "BOSS@example.com");
        when(userRepository.findByUsername("boss")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("s3cret-pass", "hashed")).thenReturn(true);

        AuthResult result = authService.login(new LoginCommand("boss", "s3cret-pass"));

        assertThat(result.roles()).containsExactly("USER", "ADMIN");
        verify(userRepository).save(user);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser("alice", "alice@example.com")));
        when(passwordHasher.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand("alice", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand("ghost", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenIssuer, never()).issue(any());
    }

    // --- refresh / rotation / reuse / logout ---

    @Test
    void refresh_validToken_rotatesAndRevokesThePresentedToken() {
        User user = loggedInUser();
        String first = login(user).refreshToken();

        AuthResult refreshed = authService.refresh(first);

        assertThat(refreshed.token()).isEqualTo("jwt-token");
        assertThat(refreshed.refreshToken()).isNotBlank().isNotEqualTo(first);
        RefreshToken old = refreshTokens.byRaw(first);
        RefreshToken successor = refreshTokens.byRaw(refreshed.refreshToken());
        assertThat(old.isRevoked()).isTrue();
        assertThat(old.getReplacedBy()).isEqualTo(successor.getId());
        assertThat(successor.getFamilyId()).isEqualTo(old.getFamilyId());
        assertThat(successor.isRevoked()).isFalse();

        // the successor works in turn
        assertThat(authService.refresh(refreshed.refreshToken()).refreshToken()).isNotBlank();
    }

    @Test
    void refresh_reusedToken_revokesTheWholeFamily() {
        User user = loggedInUser();
        String first = login(user).refreshToken();
        String second = authService.refresh(first).refreshToken();

        // an attacker (or the client) replays the already-rotated token
        assertThatThrownBy(() -> authService.refresh(first)).isInstanceOf(InvalidRefreshTokenException.class);

        // the legitimate successor has been revoked as well
        assertThat(refreshTokens.byRaw(second).isRevoked()).isTrue();
        assertThatThrownBy(() -> authService.refresh(second)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_reuseDoesNotAffectOtherSessions() {
        User user = loggedInUser();
        String sessionA = login(user).refreshToken();
        String sessionB = login(user).refreshToken();
        authService.refresh(sessionA);

        assertThatThrownBy(() -> authService.refresh(sessionA)).isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(authService.refresh(sessionB).refreshToken()).isNotBlank();
    }

    @Test
    void refresh_expiredToken_isRejected() {
        User user = loggedInUser();
        String token = login(user).refreshToken();

        clock.advance(REFRESH_TTL.plusSeconds(1));

        assertThatThrownBy(() -> authService.refresh(token)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_unknownOrBlankToken_isRejected() {
        assertThatThrownBy(() -> authService.refresh("never-issued")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authService.refresh(" ")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_reloadsRolesFromTheUserRecord() {
        User user = loggedInUser();
        String token = login(user).refreshToken();
        user.setRoles(List.of("USER", "ADMIN")); // e.g. promoted in the database meanwhile

        assertThat(authService.refresh(token).roles()).containsExactly("USER", "ADMIN");
    }

    @Test
    void refresh_lostRaceAgainstConcurrentRefresh_isTreatedAsReuse() {
        User user = loggedInUser();
        String token = login(user).refreshToken();
        refreshTokens.failNextCompareAndSet = true; // another request revoked it between our read and write

        assertThatThrownBy(() -> authService.refresh(token)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(refreshTokens.familyRevocations.get()).isEqualTo(1);
    }

    @Test
    void logout_revokesTheSessionSoRefreshFails() {
        User user = loggedInUser();
        String first = login(user).refreshToken();
        String second = authService.refresh(first).refreshToken();

        authService.logout(second);

        assertThatThrownBy(() -> authService.refresh(second)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logout_unknownToken_isANoOp() {
        authService.logout("never-issued");
        authService.logout(null);

        assertThat(refreshTokens.familyRevocations.get()).isZero();
    }

    // --- helpers ---

    private User loggedInUser() {
        User user = existingUser("alice", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("s3cret-pass", "hashed")).thenReturn(true);
        return user;
    }

    private AuthResult login(User user) {
        return authService.login(new LoginCommand(user.getUsername(), "s3cret-pass"));
    }

    private static User existingUser(String username, String email) {
        return new User(UUID.randomUUID(), username, email, "hashed", List.of("USER"), LocalDateTime.now());
    }

    private static final class CountingTokenGenerator implements OpaqueTokenGenerator {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public String generate() {
            return "rt-" + counter.incrementAndGet();
        }

        @Override
        public String hash(String rawToken) {
            return "hash:" + rawToken;
        }
    }

    /** Behaves like the JPA adapter, including the compare-and-set semantics of revokeIfActive. */
    private static final class InMemoryRefreshTokens implements RefreshTokenRepository {
        private final Map<UUID, RefreshToken> byId = new ConcurrentHashMap<>();
        final AtomicInteger familyRevocations = new AtomicInteger();
        boolean failNextCompareAndSet;

        @Override
        public RefreshToken save(RefreshToken token) {
            byId.put(token.getId(), token);
            return token;
        }

        @Override
        public Optional<RefreshToken> findByTokenHash(String tokenHash) {
            return byId.values().stream().filter(t -> t.getTokenHash().equals(tokenHash)).findFirst()
                    .map(InMemoryRefreshTokens::copy);
        }

        @Override
        public boolean revokeIfActive(UUID tokenId, Instant revokedAt, UUID replacedBy) {
            RefreshToken token = byId.get(tokenId);
            if (failNextCompareAndSet) {
                failNextCompareAndSet = false;
                token.setRevokedAt(revokedAt);
                return false;
            }
            if (token == null || token.isRevoked()) {
                return false;
            }
            token.setRevokedAt(revokedAt);
            token.setReplacedBy(replacedBy);
            return true;
        }

        @Override
        public int revokeFamily(UUID familyId, Instant revokedAt) {
            familyRevocations.incrementAndGet();
            int count = 0;
            for (RefreshToken t : byId.values()) {
                if (t.getFamilyId().equals(familyId) && !t.isRevoked()) {
                    t.setRevokedAt(revokedAt);
                    count++;
                }
            }
            return count;
        }

        RefreshToken single() {
            assertThat(byId).hasSize(1);
            return byId.values().iterator().next();
        }

        RefreshToken byRaw(String raw) {
            return byId.values().stream().filter(t -> t.getTokenHash().equals("hash:" + raw)).findFirst()
                    .orElseThrow();
        }

        private static RefreshToken copy(RefreshToken t) {
            return new RefreshToken(t.getId(), t.getUserId(), t.getFamilyId(), t.getTokenHash(),
                    t.getExpiresAt(), t.getCreatedAt(), t.getRevokedAt(), t.getReplacedBy());
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

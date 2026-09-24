package microservices.ecommerce.users.application.services;

import microservices.ecommerce.users.application.ports.in.usecases.AuthResult;
import microservices.ecommerce.users.application.ports.in.usecases.LoginCommand;
import microservices.ecommerce.users.application.ports.in.usecases.RegisterCommand;
import microservices.ecommerce.users.application.ports.out.PasswordHasher;
import microservices.ecommerce.users.application.ports.out.TokenIssuer;
import microservices.ecommerce.users.application.ports.out.UserRepository;
import microservices.ecommerce.users.core.entities.User;
import microservices.ecommerce.users.core.exceptions.InvalidCredentialsException;
import microservices.ecommerce.users.core.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_newUser_hashesPasswordSavesAndReturnsToken() {
        RegisterCommand command = new RegisterCommand("alice", "alice@example.com", "s3cret-pass");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordHasher.hash("s3cret-pass")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenIssuer.issue(any())).thenReturn("jwt-token");

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
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.username()).isEqualTo("alice");
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
    void login_validCredentials_returnsToken() {
        User user = existingUser();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("s3cret-pass", "hashed")).thenReturn(true);
        when(tokenIssuer.issue(user)).thenReturn("jwt-token");

        AuthResult result = authService.login(new LoginCommand("alice", "s3cret-pass"));

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.roles()).containsExactly("USER");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser()));
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

    private static User existingUser() {
        return new User(UUID.randomUUID(), "alice", "alice@example.com", "hashed",
                List.of("USER"), LocalDateTime.now());
    }
}

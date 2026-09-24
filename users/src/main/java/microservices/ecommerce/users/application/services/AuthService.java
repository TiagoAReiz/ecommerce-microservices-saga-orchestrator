package microservices.ecommerce.users.application.services;

import microservices.ecommerce.users.application.ports.in.usecases.AuthResult;
import microservices.ecommerce.users.application.ports.in.usecases.AuthUseCase;
import microservices.ecommerce.users.application.ports.in.usecases.LoginCommand;
import microservices.ecommerce.users.application.ports.in.usecases.RegisterCommand;
import microservices.ecommerce.users.application.ports.out.PasswordHasher;
import microservices.ecommerce.users.application.ports.out.TokenIssuer;
import microservices.ecommerce.users.application.ports.out.UserRepository;
import microservices.ecommerce.users.core.entities.User;
import microservices.ecommerce.users.core.exceptions.InvalidCredentialsException;
import microservices.ecommerce.users.core.exceptions.UserAlreadyExistsException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public AuthResult register(RegisterCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException("Username already taken");
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = new User(
                UUID.randomUUID(),
                command.username(),
                command.email(),
                passwordHasher.hash(command.rawPassword()),
                List.of(DEFAULT_ROLE),
                LocalDateTime.now());

        return authenticated(userRepository.save(user));
    }

    @Override
    public AuthResult login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .filter(u -> passwordHasher.matches(command.rawPassword(), u.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);

        return authenticated(user);
    }

    private AuthResult authenticated(User user) {
        return new AuthResult(tokenIssuer.issue(user), user.getId(), user.getUsername(), user.getRoles());
    }
}

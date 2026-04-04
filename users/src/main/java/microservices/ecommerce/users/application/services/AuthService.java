package microservices.ecommerce.users.application.services;

import microservices.ecommerce.users.application.mappers.UserMapper;
import microservices.ecommerce.users.application.ports.in.usecases.AuthUseCase;
import microservices.ecommerce.users.application.ports.out.UserRepository;
import microservices.ecommerce.users.config.JwtTokenProvider;
import microservices.ecommerce.users.core.entities.User;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.AuthResponse;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.LoginRequest;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.RegisterRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       JwtTokenProvider jwtTokenProvider,
                       BCryptPasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(List.of("USER"));
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(saved);

        return new AuthResponse(token, saved.getId(), saved.getUsername(), saved.getRoles());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRoles());
    }
}

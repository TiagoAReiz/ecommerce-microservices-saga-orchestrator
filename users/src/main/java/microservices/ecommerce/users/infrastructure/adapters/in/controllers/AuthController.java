package microservices.ecommerce.users.infrastructure.adapters.in.controllers;

import jakarta.validation.Valid;
import microservices.ecommerce.users.application.ports.in.usecases.AuthResult;
import microservices.ecommerce.users.application.ports.in.usecases.AuthUseCase;
import microservices.ecommerce.users.application.ports.in.usecases.LoginCommand;
import microservices.ecommerce.users.application.ports.in.usecases.RegisterCommand;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.AuthResponse;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.LoginRequest;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = authUseCase.register(
                new RegisterCommand(request.username(), request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authUseCase.login(new LoginCommand(request.username(), request.password()));
        return ResponseEntity.ok(toResponse(result));
    }

    private static AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(result.token(), result.userId(), result.username(), result.roles());
    }
}

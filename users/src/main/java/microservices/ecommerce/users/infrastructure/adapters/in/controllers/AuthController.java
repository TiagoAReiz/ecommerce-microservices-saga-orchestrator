package microservices.ecommerce.users.infrastructure.adapters.in.controllers;

import jakarta.validation.Valid;
import microservices.ecommerce.users.application.ports.in.usecases.AuthUseCase;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.AuthResponse;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.LoginRequest;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authUseCase.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authUseCase.login(request));
    }
}
